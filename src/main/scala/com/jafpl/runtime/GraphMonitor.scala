package com.jafpl.runtime

import java.time.{Duration, Instant}

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.{ContainerEnd, Graph, Node}
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GAbort, GCatch, GCheckGuard, GClose, GException, GFinished, GFinishedViewport, GGuardResult, GLoop, GNode, GOutput, GReset, GRun, GStart, GStop, GStopped, GTrace, GWatchdog}
import com.jafpl.runtime.NodeActor.{NAbort, NCatch, NCheckGuard, NChildFinished, NClose, NContainerFinished, NException, NGuardResult, NInitialize, NInput, NLoop, NReset, NStart, NStop, NViewportFinished}

import scala.collection.mutable

private[runtime] object GraphMonitor {
  case class GNode(node: Node, actor: ActorRef)
  case class GRun()
  case class GReset(node: Node)
  case class GStart(node: Node)
  case class GCatch(node: Node, cause: Throwable)
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

private[runtime] class GraphMonitor(private val graph: Graph, private val runtime: GraphRuntime) extends Actor {
  val log = Logging(context.system, this)
  protected val unfinishedNodes = mutable.HashSet.empty[Node]
  protected val unstoppedNodes = mutable.HashSet.empty[Node]
  private val actors = mutable.HashMap.empty[Node, ActorRef]
  private val traces = mutable.HashSet.empty[String]
  private var lastMessage = Instant.now()
  private var exception: Option[Throwable] = None

  protected def trace(message: String, event: String): Unit = {
    trace("info", message, event)
  }

  protected def trace(level: String, message: String, event: String): Unit = {
    if (traces.contains(event) || runtime.dynamicContext.traceEnabled(event)) {
      level match {
        case "info" => log.info(message)
        case "debug" => log.debug(message)
        case _ => log.warning(message)
      }
    }
  }

  // FIXME: Doing the string interpolation even when the trace won't fire is...silly

  def watchdog(millis: Long): Unit = {
    trace(s"WATCH $millis", "Watchdog")
    for (node <- unfinishedNodes) {
      trace(s"----- $node", "Watchdog")
    }
    crashAndBurn(new PipelineException("watchdog", "Watchdog timer expired", None))
  }

  def stopPipeline(): Unit = {
    for (node <- unstoppedNodes) {
      actors(node) ! NStop()
    }
  }

  def stoppedStep(node: Node): Unit = {
    unstoppedNodes -= node
    if (unstoppedNodes.isEmpty) {
      context.system.terminate()
      if (exception.isDefined) {
        runtime.finish(exception.get)
      } else {
        runtime.finish()
      }
    }
  }

  def crashAndBurn(cause: Throwable): Unit = {
    trace(s"CRASH $cause", "Exceptions")
    exception = Some(cause)
    stopPipeline()
  }

  final def receive: PartialFunction[Any, Unit] = {
    case GWatchdog(millis) =>
      val ns = Duration.between(lastMessage, Instant.now()).toMillis
      if (ns > millis) {
        watchdog(millis)
      }

    case GRun() =>
      lastMessage = Instant.now()
      trace("RUNGR", "Run")
      for (node <- graph.nodes) {
        trace(s"INITL $node", "Run")
        actors(node) ! NInitialize()
        if (node.parent.isEmpty) {
          unfinishedNodes += node
        }
      }
      for (node <- unfinishedNodes) {
        trace(s"STRTP $node", "Run")
        actors(node) ! NStart()
      }

    case GStart(node) =>
      lastMessage = Instant.now()
      trace(s"START $node", "Run")
      actors(node) ! NStart()

    case GAbort(node) =>
      lastMessage = Instant.now()
      trace(s"ABORT $node", "Run")
      actors(node) ! NAbort()

    case GStop(node) =>
      lastMessage = Instant.now()
      trace(s"STOPN $node", "Stopping")
      actors(node) ! NStop()

    case GStopped(node) =>
      lastMessage = Instant.now()
      trace(s"XXXXX $node", "Stopping")
      stoppedStep(node)

    case GCatch(node, cause) =>
      lastMessage = Instant.now()
      trace(s"START $node", "Run")
      actors(node) ! NCatch(cause)

    case GReset(node) =>
      lastMessage = Instant.now()
      trace(s"RESET $node", "Run")
      actors(node) ! NReset()

    case GOutput(node, port, item) =>
      lastMessage = Instant.now()
      trace(s"SNDTO $node.$port ($item)", "StepIO")
      val edge = node.outputEdge(port)
      actors(edge.to) ! NInput(edge.toPort, item)

    case GLoop(node, item) =>
      lastMessage = Instant.now()
      trace(s"LOOPT ($item)", "StepIO")
      actors(node) ! NLoop(item)

    case GClose(node, port) =>
      lastMessage = Instant.now()
      trace(s"CLOSE $node.$port", "StepIO")
      val edge = node.outputEdge(port)
      actors(edge.to) ! NClose(edge.toPort)

    case GCheckGuard(node) =>
      lastMessage = Instant.now()
      trace(s"GUARD $node", "Choose")
      actors(node) ! NCheckGuard()

    case GGuardResult(when, pass) =>
      lastMessage = Instant.now()
      trace(s"GRSLT $when: $pass", "Choose")
      actors(when.parent.get) ! NGuardResult(when, pass)

    case GFinished(node) =>
      lastMessage = Instant.now()
      node match {
        case end: ContainerEnd =>
          trace(s"FINSH end ${end.start.get}", "Run")
        case _ =>
          trace(s"FINSH $node", "Run")
      }

      if (unfinishedNodes.contains(node)) {
        unfinishedNodes -= node
        if (unfinishedNodes.isEmpty) {
          stopPipeline()
        }
      }

      node match {
        case end: ContainerEnd =>
          trace(s"TELLF tell start ${end.start.get} that container finished", "Run")
          actors(end.start.get) ! NContainerFinished()
        case _ =>
          if (node.parent.isDefined) {
            val end = node.parent.get.containerEnd
            trace(s"TELLF tell end ${end.start.get} that $node finished", "Run")
            actors(end) ! NChildFinished(node)
          }
      }

    case GFinishedViewport(node, buffer) =>
      lastMessage = Instant.now()
      node match {
        case end: ContainerEnd =>
          trace(s"FINSH end ${end.start.get}", "Run")
        case _ =>
          trace(s"FINSH $node", "Run")
      }

      if (unfinishedNodes.contains(node)) {
        unfinishedNodes -= node
        if (unfinishedNodes.isEmpty) {
          stopPipeline()
        }
      }

      node match {
        case end: ContainerEnd =>
          trace(s"TELLF tell start ${end.start.get} that container finished", "Run")
          actors(end.start.get) ! NViewportFinished(buffer)
        case _ =>
          if (node.parent.isDefined) {
            val end = node.parent.get.containerEnd
            trace(s"TELLF tell end ${end.start.get} that $node finished", "Run")
            actors(end) ! NChildFinished(node)
          }
      }

    case GTrace(event) =>
      lastMessage = Instant.now()
      trace(s"TRACE $event", "Traces")
      traces += event

    case GNode(node,actor) =>
      lastMessage = Instant.now()
      trace(s"ADDND $node", "AddNode")
      actors.put(node, actor)
      unstoppedNodes += node

    case GException(node, cause) =>
      lastMessage = Instant.now()
      trace(s"EXCPT $node $cause", "Exceptions")

      if (node.isDefined) {
        actors(node.get) ! NException(cause)
      } else {
        crashAndBurn(cause)
      }

    case m: Any =>
      lastMessage = Instant.now()
      log.error("Unexpected message: {}", m)
  }
}
