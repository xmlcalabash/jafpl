package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{ContainerStart, Node}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GStopped}
import com.jafpl.runtime.NodeActor.{NAbort, NCatch, NCheckGuard, NChildFinished, NClose, NContainerFinished, NException, NFinally, NGuardResult, NInitialize, NInput, NLoop, NReset, NRestartLoop, NRunFinally, NStart, NStop, NTraceDisable, NTraceEnable, NViewportFinished}
import com.jafpl.steps.{DataConsumer, Manifold, PortSpecification}

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
  case class NRestartLoop()
  case class NInput(from: Node, fromPort: String, toPort: String, item: Message)
  case class NLoop(item: ItemMessage)
  case class NClose(port: String)
  case class NChildFinished(otherNode: Node)
  case class NContainerFinished()
  case class NViewportFinished(buffer: List[Message])
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
  protected val bufferedInput: ListBuffer[InputBuffer] = ListBuffer.empty[InputBuffer]
  protected val seenBindings = mutable.HashSet.empty[String]
  protected var readyToRun = false
  protected var threwException = false
  protected var proxy = Option.empty[DataConsumer]
  protected var logEvent = TraceEvent.NODE

  def this(monitor: ActorRef, runtime: GraphRuntime, node: Node, consumer: DataConsumer) {
    this(monitor, runtime, node)
    proxy = Some(consumer)
  }

  protected def initialize(): Unit = {
    trace("INITACTOR", s"$node  step:${node.step.isDefined}", logEvent)

    for (input <- node.inputs) {
      openInputs.add(input)
    }
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

  protected def reset(): Unit = {
    trace("RESET", s"$node", logEvent)

    readyToRun = false
    threwException = false

    openInputs.clear()
    bufferedInput.clear()
    seenBindings.clear()
    for (input <- node.inputs) {
      openInputs.add(input)
    }

    node.inputCardinalities.clear()
    node.outputCardinalities.clear()

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
  }

  protected def start(): Unit = {
    trace("START", s"$node", logEvent)
    readyToRun = true
    runIfReady()
  }

  protected def abort(): Unit = {
    trace("ABORT", s"$node", logEvent)
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
    trace("STOP", s"$node", logEvent)
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

  private def runIfReady(): Unit = {
    trace("RUNIFREADY", s"$node ready:${readyToRun && !threwException} inputs:${openInputs.isEmpty}", logEvent)
    if (readyToRun && !threwException) {
      if (openInputs.isEmpty) {
        if (node.step.isDefined) {
          // Pass any statics in as normal bindings
          for ((binding,message) <- node.staticBindings) {
            if (!seenBindings.contains(binding.name)) {
              val bmsg = new BindingMessage(binding.name, message)
              node.step.get.receiveBinding(bmsg)
            }
          }
        }
        run()
      } else {
        for (port <- openInputs) {
          trace("...RWAITI", s"$node $port open", logEvent)
        }
      }
    }
  }

  protected def run(): Unit = {
    trace("RUN", s"$node", logEvent)

    readyToRun = false
    var threwException = false

    for (inj <- node.stepInjectables) {
      inj.beforeRun()
    }

    if (node.step.isDefined) {
      trace("RUNSTEP", s"$node ${node.step.get}", TraceEvent.RUNSTEP)
      try {
        node.step.get.run()

        for (input <- node.inputs.filter(!_.startsWith("#"))) {
          val count = node.inputCardinalities.getOrElse(input, 0L)
          val ispec = node.manifold.getOrElse(Manifold.ALLOW_ANY)
          ispec.inputSpec.checkInputCardinality(input, count)
        }

        for (output <- node.outputs.filter(!_.startsWith("#"))) {
          val count = node.outputCardinalities.getOrElse(output, 0L)
          val ospec = node.manifold.getOrElse(Manifold.ALLOW_ANY)
          ospec.outputSpec.checkOutputCardinality(output, count)
        }
      } catch {
        case ex: Exception =>
          threwException = true
          monitor ! GException(Some(node), ex)
      }

      trace("RAN", s"$node ${node.step.get} exception:$threwException", logEvent)

      if (!threwException) {
        for (output <- node.outputs) {
          monitor ! GClose(node, output)
        }
        monitor ! GFinished(node)
        for (inj <- node.stepInjectables) {
          inj.afterRun()
        }
      }
    } else {
      trace("RUNSTEP!", s"$node", logEvent)
      node match {
        case start: ContainerStart =>
          // Close all our "input" ports so that children reading them can run
          for (output <- node.outputs) {
            if (node.inputs.contains(output)) {
              monitor ! GClose(node, output)
            }
          }
          // Don't finish, we'll do that when our container end says all the children have finished
        case _ =>
          // For everything else, we must be atomic so we're done.
          for (output <- node.outputs) {
            monitor ! GClose(node, output)
          }
          monitor ! GFinished(node)
      }
    }
  }

  protected def close(port: String): Unit = {
    trace("CLOSE", s"$node / $port", logEvent)

    openInputs -= port
    if (port == "#bindings" && bufferedInput.nonEmpty) {
      for (buf <- bufferedInput) {
        input(buf.from, buf.fromPort, buf.port, buf.item)
      }
      bufferedInput.clear()
    }

    // We buffer inputs so that all #bindings are delivered before all other documents
    // That means we can't check cardinalities on close, we have to wait until any
    // buffered documents have been sent. The simplest rules seems to be: check
    // cardinalities after all ports are closed.
    if (openInputs.isEmpty && node.step.isDefined) {
      if (node.step.get.inputSpec != PortSpecification.ANY) {
        for (port <- node.step.get.inputSpec.ports.filter(_ != "*")) {
          try {
            val count = node.inputCardinalities.getOrElse(port, 0L)
            trace("CARD", s"$node input $port: $count", TraceEvent.CARDINALITY)
            val ispec = node.manifold.getOrElse(Manifold.ALLOW_ANY)
            ispec.inputSpec.checkInputCardinality(port, count)
          } catch {
            case ex: JafplException =>
              threwException = true
              monitor ! GException(Some(node), ex)
          }
        }
      }
    }

    runIfReady()
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
            seenBindings += binding.name
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
              node.step.get.receive(port, message)
            } else {
              trace("DELIVER↴", s"$node (no step).$port", TraceEvent.STEPIO)
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

  private def fmtSender(): String = {
    var str = sender().toString
    var pos = str.indexOf("/user/")
    str = str.substring(pos+6)
    pos = str.indexOf("#")
    str = str.substring(0, pos)
    str
  }

  final def receive: PartialFunction[Any, Unit] = {
    case NInitialize() =>
      trace("NINIT", s"$node", TraceEvent.NMESSAGES)
      initialize()

    case NInput(from, fromPort, port, item) =>
      trace("NINPUT", s"$node $from.$fromPort → $port", TraceEvent.NMESSAGES)
      trace("RECEIVE→",s"$node.$port from ${fmtSender()}", TraceEvent.STEPIO)
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
      trace("NCLOSE", s"$node.$port from ${fmtSender()}", TraceEvent.STEPIO)
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

    case NRestartLoop() =>
      trace("NRSTRTLOOP", s"$node", TraceEvent.NMESSAGES)
      this match {
        case loop: LoopEachActor => loop.restartLoop()
        case loop: LoopForActor => loop.restartLoop()
        case loop: LoopUntilActor => loop.restartLoop()
        case loop: LoopWhileActor => loop.restartLoop()
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

    case NViewportFinished(buffer) =>
      trace("NVIEWFNSHD", s"$node", TraceEvent.NMESSAGES)
      this match {
        case start: ViewportActor =>
          start.returnItems(buffer)
          start.finished()
        case _ =>
          monitor ! GException(None,
            JafplException.internalError(s"Viewport finish messages sent to something that isn't a viewport: $node", node.location))
      }

    case NChildFinished(otherNode) =>
      trace("NCHLDFNSHD", s"$node $otherNode", TraceEvent.NMESSAGES)
      this match {
        case end: EndActor =>
          end.finished(otherNode)
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
      trace("NERROR", s"$node $m", TraceEvent.NMESSAGES)
      log.error(s"Unexpected message: $m")
  }

  protected class InputBuffer(val from: Node, val fromPort: String, val port: String, val item: Message) {
  }
}
