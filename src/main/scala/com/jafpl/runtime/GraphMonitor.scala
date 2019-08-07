package com.jafpl.runtime

import java.time.{Duration, Instant}

import akka.actor.{ActorRef, PoisonPill}
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{ContainerEnd, Graph, Node}
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GAbort, GAbortExecution, GCatch, GCheckGuard, GClose, GException, GFinally, GFinished, GFinishedViewport, GGuardResult, GLoop, GNode, GOutput, GReset, GRestartLoop, GRun, GRunFinally, GStart, GStop, GStopped, GTrace, GWatchdog}
import com.jafpl.runtime.NodeActor.{NAbort, NCatch, NCheckGuard, NChildFinished, NClose, NContainerFinished, NException, NFinally, NGuardResult, NInitialize, NInput, NLoop, NReset, NRestartLoop, NRunFinally, NStart, NStop, NViewportFinished}

import scala.collection.mutable

private[runtime] object GraphMonitor {
  case class GNode(node: Node, actor: ActorRef)
  case class GRun()
  case class GAbortExecution()
  case class GReset(node: Node)
  case class GRestartLoop(node: Node)
  case class GStart(node: Node)
  case class GCatch(node: Node, cause: Throwable)
  case class GFinally(node: Node)
  case class GRunFinally(node: Node, cause: Option[Throwable])
  case class GException(node: Option[Node], cause: Throwable)
  case class GOutput(node: Node, port: String, item: Message)
  case class GLoop(node: Node, item: ItemMessage)
  case class GClose(node: Node, port: String)
  case class GFinished(node: Node)
  case class GFinishedViewport(node: Node, buffer: List[Message])
  case class GAbort(node: Node)
  case class GStop(node: Node)
  case class GStopped(node: Node)
  case class GTrace(event: String)
  case class GCheckGuard(node: Node)
  case class GGuardResult(when: Node, pass: Boolean)
  case class GWatchdog(millis: Long)
}

private[runtime] class GraphMonitor(private val graph: Graph, override protected val runtime: GraphRuntime) extends TracingActor(runtime) {
  protected val unfinishedNodes = mutable.HashSet.empty[Node]
  protected val unstoppedNodes = mutable.HashSet.empty[Node]
  private val actors = mutable.HashMap.empty[Node, ActorRef]
  private var lastMessage = Instant.now()
  private var exception: Option[Throwable] = None
  protected var logEvent = TraceEvent.MONITOR

  private def fmtSender(): String = {
    var str = sender().toString
    var pos = str.indexOf("/user/")
    str = str.substring(pos+6)
    pos = str.indexOf("#")
    if (pos > 0) {
      str = str.substring(0, pos)
    }
    str
  }

  def watchdog(millis: Long): Unit = {
    trace("WATCHDOG", s"$millis", TraceEvent.WATCHDOG)
    for (node <- unfinishedNodes) {
      trace("...UNFINSH", s"$node", TraceEvent.WATCHDOG)
    }
    crashAndBurn(JafplException.watchdogTimeout())
  }

  def stopPipeline(): Unit = {
    trace("STOPPIPE", "", logEvent)
    for (node <- unstoppedNodes) {
      if (node.parent.isEmpty) {
        actors(node) ! NStop()
      }
    }
  }

  def stoppedStep(node: Node): Unit = {
    trace("STOPDSTEP", s"$node", logEvent)
    unstoppedNodes -= node
    actors(node) ! PoisonPill
    if (unstoppedNodes.isEmpty) {
      if (exception.isDefined) {
        runtime.finish(exception.get)
      } else {
        runtime.finish()
      }
    }
  }

  def crashAndBurn(cause: Throwable): Unit = {
    trace("CRASHBURN", s"$cause", logEvent)
    exception = Some(cause)
    stopPipeline()
  }

  final def receive: PartialFunction[Any, Unit] = {
    case GWatchdog(millis) =>
      trace("GWATCHDOG", s"$millis", TraceEvent.WATCHDOG)
      val ns = Duration.between(lastMessage, Instant.now()).toMillis
      if (ns > millis) {
        watchdog(millis)
      }

    case GRun() =>
      lastMessage = Instant.now()
      trace("GRUN", "", TraceEvent.GMESSAGES)
      for (node <- graph.nodes) {
        actors(node) ! NInitialize()
        if (node.parent.isEmpty) {
          unfinishedNodes += node
        }
      }
      for (node <- unfinishedNodes) {
        actors(node) ! NStart()
      }

    case GStart(node) =>
      lastMessage = Instant.now()
      trace("GSTART", s"$node", TraceEvent.GMESSAGES)
      actors(node) ! NStart()

    case GAbort(node) =>
      lastMessage = Instant.now()
      trace("GABORT", s"$node", TraceEvent.GMESSAGES)
      actors(node) ! NAbort()

    case GStop(node) =>
      lastMessage = Instant.now()
      trace("GSTOP", s"$node", TraceEvent.GMESSAGES)
      actors(node) ! NStop()

    case GStopped(node) =>
      lastMessage = Instant.now()
      trace("GSTOPPED", s"$node", TraceEvent.GMESSAGES)
      stoppedStep(node)

    case GCatch(node, cause) =>
      lastMessage = Instant.now()
      trace("GCATCH", s"$node $cause", TraceEvent.GMESSAGES)
      actors(node) ! NCatch(cause)

    case GFinally(node) =>
      lastMessage = Instant.now()
      trace("GFINALLY", s"$node", TraceEvent.GMESSAGES)
      actors(node) ! NFinally()

    case GRunFinally(node, cause) =>
      lastMessage = Instant.now()
      trace("GRUNFINAL", s"$node $cause", TraceEvent.GMESSAGES)
      actors(node) ! NRunFinally(cause)

    case GReset(node) =>
      lastMessage = Instant.now()
      trace("GRESET", s"$node", TraceEvent.GMESSAGES)
      actors(node) ! NReset()

    case GRestartLoop(node) =>
      lastMessage = Instant.now()
      trace("GRSTRTLOOP", s"$node", TraceEvent.GMESSAGES)
      actors(node) ! NRestartLoop()

    case GOutput(node, port, item) =>
      lastMessage = Instant.now()
      trace("GOUTPUT", s"$node.$port", TraceEvent.GMESSAGES)

      item match {
        case msg: ItemMessage =>
          for (inj <- node.outputInjectables) {
            if (inj.port == port) {
              inj.run(msg)
            }
          }
        case _ => Unit
      }

      if (node.hasOutputEdge(port)) {
        val edge = node.outputEdge(port)
        trace("SENDOUT→", s"$node.$port → ${edge.to}.${edge.toPort} from ${fmtSender()}", TraceEvent.STEPIO)
        trace("MESSAGE→", s"$node.$port → ${edge.to}.${edge.toPort} $item", TraceEvent.MESSAGE)
        actors(edge.to) ! NInput(node, port, edge.toPort, item)
      } else {
        trace("DROPOUT↴", s"$node.$port from ${fmtSender()}", TraceEvent.STEPIO)
        trace("MESSAGE↴", s"$node.$port $item", TraceEvent.MESSAGE)
      }

    case GLoop(node, item) =>
      lastMessage = Instant.now()
      trace("GLOOP", s"$node ($item)", TraceEvent.GMESSAGES)
      actors(node) ! NLoop(item)

    case GClose(node, port) =>
      lastMessage = Instant.now()
      trace("GCLOSE", s"$node.$port", TraceEvent.GMESSAGES)
      trace("GCLOSE", s"$node.$port from ${fmtSender()}", TraceEvent.STEPIO)
      val edge = node.outputEdge(port)
      actors(edge.to) ! NClose(edge.toPort)

    case GCheckGuard(node) =>
      lastMessage = Instant.now()
      trace("GCHKGUARD", s"$node", TraceEvent.GMESSAGES)
      actors(node) ! NCheckGuard()

    case GGuardResult(when, pass) =>
      lastMessage = Instant.now()
      trace("GGUARDRES", s"$when: $pass", TraceEvent.GMESSAGES)
      actors(when.parent.get) ! NGuardResult(when, pass)

    case GFinished(node) =>
      lastMessage = Instant.now()
      trace("GFINISHED", s"$node", TraceEvent.GMESSAGES)

      if (unfinishedNodes.contains(node)) {
        unfinishedNodes -= node
        if (unfinishedNodes.isEmpty) {
          stopPipeline()
        }
      }

      node match {
        case end: ContainerEnd =>
          actors(end.start.get) ! NContainerFinished()
        case _ =>
          if (node.parent.isDefined) {
            val end = node.parent.get.containerEnd
            actors(end) ! NChildFinished(node)
          }
      }

    case GFinishedViewport(node, buffer) =>
      lastMessage = Instant.now()
      trace("GFINVIEWPRT", s"$node", TraceEvent.GMESSAGES)

      node match {
        case end: ContainerEnd =>
          actors(end.start.get) ! NViewportFinished(buffer)
        case _ =>
          if (node.parent.isDefined) {
            val end = node.parent.get.containerEnd
            actors(end) ! NChildFinished(node)
          }
      }

    case GTrace(event) =>
      lastMessage = Instant.now()
      trace("GTRACE", s"$event", TraceEvent.TRACES)
      runtime.traceEventManager.enableTrace(event)

    case GNode(node,actor) =>
      lastMessage = Instant.now()
      trace("GNODE", s"$node", TraceEvent.GMESSAGES)
      actors.put(node, actor)
      unstoppedNodes += node

    case GException(node, cause) =>
      lastMessage = Instant.now()
      trace("GEXCEPT", s"$node $cause", TraceEvent.GMESSAGES)

      if (node.isDefined) {
        actors(node.get) ! NException(cause)
      } else {
        crashAndBurn(cause)
      }

    case GAbortExecution() =>
      lastMessage = Instant.now()
      trace("GABORTEXEC", "", TraceEvent.GMESSAGES)
      stopPipeline()

    case m: Any =>
      lastMessage = Instant.now()
      trace("GERROR", s"$m", TraceEvent.GMESSAGES)
      log.error(s"UNEXPECT $m")
  }
}
