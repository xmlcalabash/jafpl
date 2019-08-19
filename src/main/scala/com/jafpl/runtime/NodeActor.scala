package com.jafpl.runtime

import akka.actor.ActorRef
import akka.event.LoggingReceive
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{ContainerStart, Node}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GAbort, GClose, GException, GFinished, GResetFinished, GStopped}
import com.jafpl.runtime.NodeActor.{NAbort, NCatch, NCheckGuard, NChildFinished, NClose, NContainerFinished, NException, NFinally, NGuardResult, NInitialize, NInput, NLoop, NReset, NResetFinished, NRestartLoop, NRunFinally, NStart, NStop, NTraceDisable, NTraceEnable}
import com.jafpl.runtime.NodeState.NodeState
import com.jafpl.runtime.TraceEvent.TraceEvent
import com.jafpl.steps.{DataConsumer, Manifold}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] object NodeActor {
  case class NInitialize()
  case class NStart()
  case class NAbort()
  case class NStop()
  case class NCatch(cause: Throwable)
  case class NFinally()
  case class NRunFinally(cause: Option[Throwable])
  case class NReset()
  case class NResetFinished(node: Node)
  case class NRestartLoop()
  case class NInput(from: Node, fromPort: String, toPort: String, item: Message)
  case class NLoop(item: ItemMessage)
  case class NClose(port: String)
  case class NChildFinished(otherNode: Node)
  case class NContainerFinished()
  case class NTraceEnable(event: String)
  case class NTraceDisable(event: String)
  case class NCheckGuard()
  case class NGuardResult(when: Node, pass: Boolean)
  case class NException(cause: Throwable)
}

private[runtime] class NodeActor(private val monitor: ActorRef,
                                 override protected val runtime: GraphRuntime,
                                 protected val node: Node) extends TracingActor(runtime) {
  protected val openInputs = mutable.HashSet.empty[String]
  protected val openOutputs = mutable.HashSet.empty[String]
  protected val childState = mutable.HashMap.empty[Node, NodeState]

  protected val bufferedInput: ListBuffer[InputBuffer] = ListBuffer.empty[InputBuffer]
  protected val receivedBindings = mutable.HashSet.empty[String]

  protected var started = false
  protected var threwException = false
  protected var aborted = false

  protected var proxy = Option.empty[DataConsumer]
  protected var logEvent: TraceEvent = TraceEvent.NODE

  def this(monitor: ActorRef, runtime: GraphRuntime, node: Node, consumer: DataConsumer) {
    this(monitor, runtime, node)
    proxy = Some(consumer)
  }

  protected def childrenHaveReset: Boolean = {
    for (state <- childState.values) {
      if (state != NodeState.RESET && state != NodeState.FINISHED) {
        return false
      }
    }
    true
  }

  protected def childrenHaveFinished: Boolean = {
    for (state <- childState.values) {
      if (state != NodeState.FINISHED && state != NodeState.STOPPED && state != NodeState.ABORTED) {
        return false
      }
    }
    true
  }

  protected def readyToRun: Boolean = {
    // startedChildren has to be empty, right? What would it mean if it wasn't?
   started && !aborted && !threwException && openInputs.isEmpty && childrenHaveReset
  }

  protected def initialize(): Unit = {
    configureOpenPorts()

    node.state = NodeState.INIT
    if (node.step.isDefined) {
      try {
        node.step.get.initialize(runtime.runtime)
      } catch {
        case cause: Throwable =>
          threwException = true
          monitor ! GException(Some(node), cause)
      }
    }
  }

  protected def configureOpenPorts(): Unit = {
    openInputs.clear()
    openOutputs.clear()

    for (input <- node.inputs) {
      openInputs.add(input)
    }

    for (output <- node.outputs) {
      openOutputs.add(output)
      if (openInputs.contains(output)) {
        openInputs -= output
      }
    }

    node.inputCardinalities.clear()
    node.outputCardinalities.clear()
  }

  protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node / $fromPort to $port", logEvent)

    if (port != "#bindings" && openInputs.contains("#bindings")) {
      bufferedInput += new InputBuffer(from, fromPort, port, item)
      return
    }

    try {
      if (port == "#bindings") {
        item match {
          case binding: BindingMessage =>
            for (inj <- node.inputInjectables ++ node.outputInjectables) {
              inj.receiveBinding(binding)
            }
            for (inj <- node.stepInjectables) {
              inj.receiveBinding(binding)
            }
            receivedBindings += binding.name
            if (node.step.isDefined) {
              trace("BINDING→", s"$node: ${binding.name}=${binding.message}", TraceEvent.BINDINGS)
              node.step.get.receiveBinding(binding)
            } else {
              trace("BINDING↴", s"$node: ${binding.name}=${binding.message}", TraceEvent.BINDINGS)
            }
          case _ =>
            throw JafplException.unexpectedMessage(item.toString, "#bindings", node.location)
        }
      } else {
        item match {
          case message: ItemMessage =>
            for (inj <- node.inputInjectables) {
              if (inj.port == port) {
                inj.run(message)
              }
            }
            node.inputCardinalities.put(port, node.inputCardinalities.getOrElse(port, 0L) + 1)
            if (node.step.isDefined) {
              trace("DELIVER→", s"$node ${node.step.get}.$port", TraceEvent.STEPIO)
              trace("MESSAGE→", s"$node ${node.step.get}.$port $item", TraceEvent.MESSAGE)
              node.step.get.consume(port, message)
            } else {
              trace("DELIVER↴", s"$node (no step).$port", TraceEvent.STEPIO)
              trace("MESSAGE↴", s"$node (no step).$port $item", TraceEvent.MESSAGE)
            }
          case _ =>
            throw JafplException.unexpectedMessage(item.toString, port, node.location)
        }
      }
    } catch {
      case t: Throwable =>
        threwException = true
        monitor ! GException(None, t)
    }
  }

  protected def close(port: String): Unit = {
    trace("CLOSEI", s"$node / $port", logEvent)

    openInputs -= port
    if (port == "#bindings" && bufferedInput.nonEmpty) {
      for (buf <- bufferedInput) {
        input(buf.from, buf.fromPort, buf.port, buf.item)
      }
      bufferedInput.clear()
    }

    runIfReady()
  }

  protected def start(): Unit = {
    for (inj <- node.stepInjectables) {
      inj.beforeRun()
    }
    node.state = NodeState.STARTED
    started = true
    runIfReady()
  }

  protected def abort(): Unit = {
    trace("ABORT", s"$node", logEvent)
    node.state = NodeState.ABORTED
    aborted = true

    if (node.step.isDefined) {
      trace("ABORTSTEP", s"$node ${node.step.get}", logEvent)
      try {
        node.step.get.abort()
      } catch {
        case cause: Throwable =>
          threwException = true
          monitor ! GException(Some(node), cause)
      }
    } else {
      trace("ABORTSTEP!", s"$node", logEvent)
    }

    monitor ! GFinished(node)
  }

  protected def stop(): Unit = {
    node.state = NodeState.STOPPED

    if (node.step.isDefined) {
      trace("STOPSTEP", s"$node ${node.step.get}", logEvent)
      try {
        node.step.get.stop()
      } catch {
        case cause: Throwable =>
          threwException = true
          monitor ! GException(Some(node), cause)
      }
    } else {
      trace("STOPSTEP!", s"$node", logEvent)
    }

    monitor ! GStopped(node)
  }

  protected def reset(): Unit = {
    node.state = NodeState.RESETTING
    started = false
    aborted = false
    threwException = false

    bufferedInput.clear()
    receivedBindings.clear()

    configureOpenPorts()

    if (node.step.isDefined) {
      trace("RESETNODE", s"$node", logEvent)
      try {
        node.step.get.reset()
      } catch {
        case cause: Throwable =>
          threwException = true
          monitor ! GException(Some(node), cause)
      }
    }

    resetIfReady()
  }

  protected def resetIfReady(): Unit = {
    monitor ! GResetFinished(node)
  }

  protected def resetFinished(node: Node): Unit = {
    node.state = NodeState.RESET
  }

  protected def runIfReady(): Unit = {
    val iready = openInputs.isEmpty

    trace("RUNIFREADY", s"$node ready: $readyToRun a:$aborted s:$started, c:$childrenHaveReset inputs: $iready resets: $childrenHaveReset", logEvent)

    if (openInputs.nonEmpty) {
      for (port <- openInputs) {
        trace("...RWAITI", s"$node $port open", logEvent)
      }
    }

    for ((node,state) <- childState) {
      if (state != NodeState.RESET) {
        trace("...RWAITR", s"$node resetting ($state)", logEvent)
      }
    }

    if (!readyToRun) {
      return
    }

    if (node.step.isDefined) {
      // Pass any statics in as normal bindings
      for ((binding,message) <- node.staticBindings) {
        if (!receivedBindings.contains(binding.name)) {
          val bmsg = new BindingMessage(binding.name, message)
          node.step.get.receiveBinding(bmsg)
        }
      }
    }

    run()
  }

  protected def run(): Unit = {
    trace("RUN", s"$node", logEvent)

    started = false
    node.state = NodeState.FINISHED

    for (inj <- node.stepInjectables) {
      inj.beforeRun()
    }

    if (node.step.isDefined) {
      try {
        for (port <- node.inputs.filter(!_.startsWith("#"))) {
          val count = node.inputCardinalities.getOrElse(port, 0L)
          val ispec = node.manifold.getOrElse(Manifold.ALLOW_ANY)
          trace("CARD", s"$node input $port: $count", TraceEvent.CARDINALITY)
          ispec.inputSpec.checkInputCardinality(port, count)
        }

        node.step.get.run()

        for (port <- node.outputs.filter(!_.startsWith("#"))) {
          val count = node.outputCardinalities.getOrElse(port, 0L)
          val ospec = node.manifold.getOrElse(Manifold.ALLOW_ANY)
          ospec.outputSpec.checkOutputCardinality(port, count)
        }
      } catch {
        case ex: Exception =>
          threwException = true
          monitor ! GException(Some(node), ex)
      }

      if (!threwException) {
        for (inj <- node.stepInjectables) {
          inj.afterRun()
        }
      }
    } else {
      node match {
        case _: ContainerStart =>
          // Close all our "input" ports so that children reading them can run
          for (output <- node.outputs) {
            if (node.inputs.contains(output)) {
              monitor ! GClose(node, output)
            }
          }
        case _ => Unit
      }
    }

    for (output <- node.outputs) {
      monitor ! GClose(node, output)
    }

    if (threwException) {
      monitor ! GAbort(node)
    } else {
      monitor ! GFinished(node)
    }
  }

  private def fmtSender: String = {
    var str = sender().toString
    var pos = str.indexOf("/user/")
    str = str.substring(pos+6)
    pos = str.indexOf("#")
    str = str.substring(0, pos)
    str
  }

  final def receive: PartialFunction[Any, Unit] = {
    LoggingReceive {
      case NInitialize() =>
        trace("NINIT", s"$node", TraceEvent.NMESSAGES)
        initialize()

      case NInput(from, fromPort, port, item) =>
        trace("NINPUT", s"$node $from.$fromPort → $port", TraceEvent.NMESSAGES)
        trace("RECEIVE→",s"$node.$port from $fmtSender ITEM: $item", TraceEvent.STEPIO)
        input(from, fromPort, port, item)

      case NLoop(item) =>
        trace("NLOOP", s"$node $item", TraceEvent.NMESSAGES)
        this match {
          case loop: LoopUntilActor =>
            loop.loop(item)
          case loop: LoopWhileActor =>
            loop.loop(item)
          case _ =>
            monitor ! GException(None, JafplException.internalError(s"Invalid loop to $node", node.location))
        }

      case NClose(port) =>
        trace("NCLOSE", s"$node $port", TraceEvent.NMESSAGES)
        close(port)

      case NStart() =>
        trace("NSTART", s"$node", TraceEvent.NMESSAGES)
        start()

      case NAbort() =>
        trace("NABORT", s"$node", TraceEvent.NMESSAGES)
        abort()

      case NStop() =>
        trace("NSTOP", s"$node", TraceEvent.NMESSAGES)
        stop()

      case NCatch(cause) =>
        trace("NCATCH", s"$node", TraceEvent.NMESSAGES)
        this match {
          case catchStep: CatchActor =>
            catchStep.start(cause)
          case _ =>
            monitor ! GException(None,
              JafplException.internalError("Attempt to send exception to something that's not a catch", node.location))
        }

      case NFinally() =>
        trace("NFINALLY", s"$node", TraceEvent.NMESSAGES)
        this match {
          case block: TryCatchActor =>
            block.runFinally()
          case _ =>
            monitor ! GException(None,
              JafplException.internalError("Attempt to send finally to something that's not a try/catch", node.location))
        }

      case NRunFinally(cause) =>
        trace("NRUNFINAL", s"$node $cause", TraceEvent.NMESSAGES)
        this match {
          case block: FinallyActor =>
            block.startFinally(cause)
          case _ =>
            monitor ! GException(None,
              JafplException.internalError("Attempt to send run_finally to something that's not a finally", node.location))
        }

      case NReset() =>
        trace("NRESET", s"$node", TraceEvent.NMESSAGES)
        reset()

      case NResetFinished(child) =>
        trace("NRESETF", s"$child", TraceEvent.NMESSAGES)
        resetFinished(child)

      case NRestartLoop() =>
        trace("NRSTRTLOOP", s"$node", TraceEvent.NMESSAGES)
        this match {
          case loop: LoopEachActor => loop.restartLoop()
          case loop: LoopForActor => loop.restartLoop()
          case loop: LoopUntilActor => loop.restartLoop()
          case loop: LoopWhileActor => loop.restartLoop()
          case loop: ViewportActor => loop.restartLoop()
          case _ =>
            monitor ! GException(None, JafplException.internalError(s"Attempt to restart non-loop $node", node.location))
        }

      case NContainerFinished() =>
        trace("NCONTFNSHD", s"$node", TraceEvent.NMESSAGES)
        this match {
          case start: StartActor =>
            start.finished()
          case _ =>
            monitor ! GException(None,
              JafplException.internalError(s"Container finish message sent to something that isn't a start: $node", node.location))
        }

      case NChildFinished(otherNode) =>
        trace("NCHLDFNSH ", s"$node $otherNode / ${otherNode.state}", TraceEvent.NMESSAGES)
        this match {
          case start: StartActor =>
            start.childFinished(otherNode)
          case _ =>
            monitor ! GException(None,
              JafplException.internalError(s"Child finish message sent to something that isn't an end: $node", node.location))
        }

      case NCheckGuard() =>
        trace("NCHKGUARD", s"$node", TraceEvent.NMESSAGES)
        this match {
          case when: WhenActor =>
            when.checkGuard()
          case _ =>
            monitor ! GException(None,
              JafplException.internalError(s"Attept to check guard expresson on something that isn't a when: $node", node.location))
        }

      case NGuardResult(when, pass) =>
        trace("NGUARDRES", s"$node $when: $pass", TraceEvent.NMESSAGES)
        this match {
          case choose: ChooseActor =>
            choose.guardResult(when, pass)
          case _ =>
            monitor ! GException(None,
              JafplException.internalError(s"Attempt to pass guard result to something that isn't a when: $node", node.location))
        }

      case NException(cause) =>
        trace("NEXCEPT", s"$node $cause ($this)", TraceEvent.NMESSAGES)
        this match {
          case trycatch: TryCatchActor =>
            trycatch.exception(cause)
          case tryblock: TryActor =>
            tryblock.exception(cause)
          case _ =>
            monitor ! GException(node.parent, cause)
        }

      case NTraceEnable(event) =>
        trace("TRACE+", s"$node $event", TraceEvent.TRACES)
        runtime.traceEventManager.enableTrace(event)

      case NTraceDisable(event) =>
        trace("TRACE-", s"$node $event", TraceEvent.TRACES)
        runtime.traceEventManager.disableTrace(event)

      case m: Any =>
        trace("NERROR", s"$node $fmtSender: $m", TraceEvent.NMESSAGES)
        log.error(s"Unexpected message: $m")
    }
  }

  protected class InputBuffer(val from: Node, val fromPort: String, val port: String, val item: Message) {
  }
}
