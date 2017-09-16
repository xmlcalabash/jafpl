package com.jafpl.runtime

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.{ContainerStart, Node}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GStopped}
import com.jafpl.runtime.NodeActor.{NAbort, NCatch, NCheckGuard, NChildFinished, NClose, NContainerFinished, NException, NFinally, NGuardResult, NInitialize, NInput, NLoop, NReset, NRunFinally, NStart, NStop, NTraceDisable, NTraceEnable, NViewportFinished}
import com.jafpl.steps.{DataConsumer, PortSpecification}

import scala.collection.mutable

private[runtime] object NodeActor {
  case class NInitialize()
  case class NStart()
  case class NAbort()
  case class NStop()
  case class NCatch(cause: Throwable)
  case class NFinally()
  case class NRunFinally(cause: Option[Throwable])
  case class NReset()
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
                                 private val runtime: GraphRuntime,
                                 private val node: Node) extends Actor {
  protected val log = Logging(context.system, this)
  protected val openInputs = mutable.HashSet.empty[String]
  protected val openBindings = mutable.HashSet.empty[String]
  protected var readyToRun = false
  protected val cardinalities = mutable.HashMap.empty[String, Long]
  protected var proxy = Option.empty[DataConsumer]

  def this(monitor: ActorRef, runtime: GraphRuntime, node: Node, consumer: DataConsumer) {
    this(monitor, runtime, node)
    proxy = Some(consumer)
  }

  protected def trace(message: String, event: String): Unit = {
    trace("info", message, event)
  }

  protected def trace(level: String, message: String, event: String): Unit = {
    // We don't use the traceEventManager.trace() call because we want to use the Akka logger
    if (runtime.traceEventManager.traceEnabled(event)) {
      level match {
        case "info" => log.info(message)
        case "debug" => log.debug(message)
        case _ => log.warning(message)
      }
    }
  }

  protected def initialize(): Unit = {
    for (input <- node.inputs) {
      openInputs.add(input)
    }
    for (input <- node.bindings) {
      openBindings.add(input)
    }
    if (node.step.isDefined) {
      trace(s"INITSTEP $node", "StepExec")
      try {
        node.step.get.initialize(runtime.runtime)
      } catch {
        case cause: Throwable =>
          monitor ! GException(Some(node), cause)
      }
    }
  }

  protected def reset(): Unit = {
    readyToRun = false
    openInputs.clear()
    for (input <- node.inputs) {
      openInputs.add(input)
    }
    openBindings.clear()
    for (input <- node.bindings) {
      openBindings.add(input)
    }
    cardinalities.clear()
    if (proxy.isDefined) {
      proxy.get match {
        case cp: ConsumingProxy =>
          cp.reset()
        case _ => Unit
      }
    }
    if (node.step.isDefined) {
      trace(s"RESETNOD $node", "StepExec")
      try {
        node.step.get.reset()
      } catch {
        case cause: Throwable =>
          monitor ! GException(Some(node), cause)
      }
    }
  }

  protected def start(): Unit = {
    readyToRun = true
    runIfReady()
  }

  protected def abort(): Unit = {
    if (node.step.isDefined) {
      trace(s"ABORTSTP $node", "StepExec")
      try {
        node.step.get.abort()
      } catch {
        case cause: Throwable =>
          monitor ! GException(Some(node), cause)
      }
    } else {
      trace(s"ABORT___ $node", "StepExec")
    }
    monitor ! GFinished(node)
  }

  protected def stop(): Unit = {
    if (node.step.isDefined) {
      trace(s"STOPSTEP $node", "Stopping")
      try {
        node.step.get.stop()
      } catch {
        case cause: Throwable =>
          monitor ! GException(Some(node), cause)
      }
    } else {
      trace(s"STOPXSTP $node", "Stopping")
    }

    monitor ! GStopped(node)
  }

  private def runIfReady(): Unit = {
    trace(s"RUNIFRDY $node (ready:$readyToRun inputs:${openInputs.isEmpty} bindings:${openBindings.isEmpty})", "StepExec")

    if (readyToRun) {
      if (openInputs.isEmpty && openBindings.isEmpty) {
        run()
      } else {
        for (port <- openInputs) {
          trace(s"........ $port open", "StepExec")
        }
        for (varname <- openBindings) {
          trace(s"........ $varname binding open", "StepExec")
        }
      }
    }
  }

  protected def run(): Unit = {
    readyToRun = false
    var threwException = false

    if (node.step.isDefined) {
      trace(s"RUNSTEP  $node", "StepExec")
      try {
        node.step.get.run()
        if (proxy.isDefined) {
          for (output <- node.outputs) {
            if (!output.startsWith("#")) {
              proxy.get match {
                case cp: ConsumingProxy => node.step.get.outputSpec.checkCardinality(output,cp.cardinality(output))
                case _ => Unit
              }
            }
          }
        }
      } catch {
        case pipeex: PipelineException =>
          threwException = true
          monitor ! GException(Some(node), pipeex)
        case cause: Throwable =>
          threwException = true
          monitor ! GException(Some(node), cause)
      }

      trace(s"Ran $node: $threwException", "StepExec")
      if (!threwException) {
        for (output <- node.outputs) {
          trace(s"Closing $output for $node", "StepExec")
          monitor ! GClose(node, output)
        }
        monitor ! GFinished(node)
      }
    } else {
      trace(s"RUN____  $node", "StepExec")
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
    if (node.step.isDefined && node.step.get.inputSpec != PortSpecification.ANY
        && !port.startsWith("#")) {
      try {
        node.step.get.inputSpec.checkCardinality(port, cardinalities.getOrElse(port, 0L))
      } catch {
        case cause: Throwable =>
          monitor ! GException(Some(node), cause)
        case _: Throwable => Unit
      }
    }
    openInputs -= port
    runIfReady()
  }

  protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    if (port == "#bindings") {
      item match {
        case binding: BindingMessage =>
          if (node.step.isDefined) {
            trace(s"→BINDING $node: ${binding.name}=${binding.message}", "Bindings")
            node.step.get.receiveBinding(binding)
          } else {
            trace(s"↴BINDING $node: ${binding.name}=${binding.message} (no step)", "Bindings")
          }
          openBindings -= binding.name
        case _ =>
          monitor ! GException(None,
            new PipelineException("badbinding", s"Unexpected message $item on #bindings port", node.location))
          return
      }
    } else {
      item match {
        case message: ItemMessage =>
          val card = cardinalities.getOrElse(port, 0L) + 1L
          cardinalities.put(port, card)
          if (node.step.isDefined) {
            trace(s"DELIVER→ ${node.step.get}.$port", "StepIO")
            runtime.runtime.deliver(from.id, fromPort, message, node.step.get, port)
          } else {
            trace(s"↴DELIVER $node (no step)", "StepIO")
          }
        case _ =>
          monitor ! GException(None,
            new PipelineException("badmessage", s"Unexpected message $item on port $port", node.location))
          return
      }
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
      trace(s"INITLIZE $node", "StepMessages")
      initialize()

    case NInput(from, fromPort, port, item) =>
      trace(s"→RECEIVE $node.$port from ${fmtSender()}", "StepIO")
      input(from, fromPort, port, item)

    case NLoop(item) =>
      trace(s"LOOPSTRT $item", "StepIO")
      this match {
        case loop: LoopUntilActor =>
          loop.loop(item)
        case loop: LoopWhileActor =>
          loop.loop(item)
        case _ =>
          monitor ! GException(None, new PipelineException("invloop", s"Invalid loop to $node", node.location))
      }

    case NClose(port) =>
      trace(s"CLOSEOUT $node.$port", "StepIO")
      close(port)

    case NStart() =>
      trace(s"RUNSTART $node", "StepMessages")
      start()

    case NAbort() =>
      trace(s"ABORTNOD $node", "StepMessages")
      abort()

    case NStop() =>
      trace(s"STOPNODE $node", "Stopping")
      stop()

    case NCatch(cause) =>
      trace(s"RUNCATCH $node", "StepMessages")
      this match {
        case catchStep: CatchActor =>
          catchStep.start(cause)
        case _ =>
          monitor ! GException(None,
            new PipelineException("notcatch", "Attempt to send exception to something that's not a catch", node.location))
      }

    case NFinally() =>
      trace(s"FINALLY $node", "StepMessages")
      this match {
        case block: TryCatchActor =>
          block.runFinally()
        case _ =>
          monitor ! GException(None,
            new PipelineException("nottry", s"Attempt to send finally to something that's not a try/catch: $this", node.location))
      }

    case NRunFinally(cause) =>
      trace(s"RUNFINAL $node ($cause)", "StepMessages")
      this match {
        case block: FinallyActor =>
          block.startFinally(cause)
        case _ =>
          monitor ! GException(None,
            new PipelineException("notfinally", "Attempt to send run finally to something that's not a finally", node.location))
      }

    case NReset() =>
      trace(s"RESETNOD $node", "StepMessages")
      reset()

    case NContainerFinished() =>
      trace(s"CNTNRFIN $node", "StepMessages")

      this match {
        case start: StartActor =>
          start.finished()
        case _ =>
          monitor ! GException(None,
            new PipelineException("notstart", s"Container finish message sent to $node", node.location))
      }

    case NViewportFinished(buffer) =>
      trace(s"VIEWPFIN $node", "StepMessages")

      this match {
        case start: ViewportActor =>
          start.returnItems(buffer)
          start.finished()
        case _ =>
          monitor ! GException(None,
            new PipelineException("notviewport", s"Viewport finish message sent to $node", node.location))
      }

    case NChildFinished(otherNode) =>
      trace(s"CHILDFIN $otherNode", "StepMessages")
      this match {
        case end: EndActor =>
          end.finished(otherNode)
        case _ =>
          monitor ! GException(None,
            new PipelineException("notend", s"Child finish message sent to $node", node.location))
      }

    case NCheckGuard() =>
      trace(s"CHKGUARD", "StepMessages")
      this match {
        case when: WhenActor =>
          when.checkGuard()
        case _ =>
          monitor ! GException(None,
            new PipelineException("badguard", "Attempted to check guard on something that isn't a when: " + this, node.location))
      }

    case NGuardResult(when, pass) =>
      trace(s"GRDRESLT $when: $pass", "StepMessages")
      this match {
        case choose: ChooseActor =>
          choose.guardResult(when, pass)
        case _ =>
          monitor ! GException(None,
            new PipelineException("badguard", "Attempted to pass guard result to something that isn't a when", node.location))
      }

    case NException(cause) =>
      trace(s"EXCPTION $node $cause $this", "StepMessages")
      this match {
        case trycatch: TryCatchActor =>
          trycatch.exception(cause)
        case _ =>
          monitor ! GException(node.parent, cause)
      }

    case NTraceEnable(event) =>
      trace(s"TRACEADD $event", "Traces")
      runtime.traceEventManager.enableTrace(event)

    case NTraceDisable(event) =>
      trace(s"TRACERMV $event", "Traces")
      runtime.traceEventManager.disableTrace(event)

    case m: Any =>
      log.error(s"Unexpected message: $m")
  }
}
