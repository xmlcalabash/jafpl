package com.jafpl.runtime

import java.time.{Duration, Instant}

import akka.actor.{ActorRef, PoisonPill}
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{ContainerStart, Graph, Node, NodeState}
import com.jafpl.runtime.NodeActor.{NAbortExecution, NException, NFinished, NInitialize, NInitialized, NNode, NReady, NRunIfReady, NRunning, NStart, NStarted, NStop, NStopped, NWatchdog, NWatchdogTimeout}

import scala.collection.mutable

private[runtime] class GraphMonitor(private val graph: Graph, override protected val runtime: GraphRuntime) extends TracingActor(runtime) {
  protected val allNodes = mutable.HashSet.empty[Node]
  protected val topLevelNodes = mutable.HashSet.empty[Node]
  private val actors = mutable.HashMap.empty[Node, ActorRef]
  private var lastMessage = Instant.now()
  private var exception: Option[Exception] = None
  protected var logEvent = TraceEvent.MONITOR

  def watchdog(millis: Long): Unit = {
    trace("WATCHDOG", s"$millis", TraceEvent.WATCHDOG)
    for (node <- allNodes) {
      trace("...WATCHDOG", s"$node: ${node.state}", TraceEvent.WATCHDOG)
    }
    crashAndBurn(JafplException.watchdogTimeout())
  }

  def crashAndBurn(cause: Exception): Unit = {
    trace("CRASHBURN", s"$cause", logEvent)
    exception = Some(cause)
    stopPipeline()
  }

  def stop(): Unit = {
    stopPipeline()

    // What if we got stopped before we really got started?
    if (topLevelNodes.isEmpty) {
      runtime.finish()
    }
  }

  def stopPipeline(): Unit = {
    trace("STOPPIPE", "", logEvent)
    for (node <- topLevelNodes) {
      stateChange(node, NodeState.STOPPING)
      actors(node) ! NStop()
    }
  }

  def stopped(node: Node): Unit = {
    stateChange(node, NodeState.STOPPED)
    topLevelNodes -= node
    actors(node) ! PoisonPill
    if (topLevelNodes.isEmpty) {
      if (exception.isDefined) {
        runtime.finish(exception.get)
      } else {
        runtime.finish()
      }
    }
  }

  def finished(node: Node): Unit = {
    stateChange(node, NodeState.STOPPING)
    actors(node) ! NStop()
  }

  def initialize(node: Node): Unit = {
    val pref: Option[ActorRef] = if (node.parent.isDefined) {
      Some(actors(node.parent.get))
    } else {
      None
    }

    val pmap = mutable.HashMap.empty[String,(String,ActorRef)]
    for (port <- node.outputs) {
      if (node.hasOutputEdge(port)) {
        val edge = node.outputEdge(port)
        pmap(port) = (edge.toPort, actors(edge.to))
      }
    }

    node match {
      case start: ContainerStart =>
        val cmap = mutable.HashMap.empty[Node,ActorRef]
        for (child <- start.children) {
          cmap(child) = actors(child)
        }
        actors(node) ! NInitialize(pref, cmap.toMap, pmap.toMap)
      case _ =>
        actors(node) ! NInitialize(pref, Map(), pmap.toMap)
    }

  }

  def initialized(node: Node): Unit = {
    stateChange(node, NodeState.INIT)
    var ready = true
    for (node <- allNodes) {
      ready = ready && node.state == NodeState.INIT
    }
    if (ready) {
      for (node <- topLevelNodes) {
        stateChange(node, NodeState.STARTING)
        actors(node) ! NStart()
      }
    }
  }

  def ready(node: Node): Unit = {
    actors(node) ! NRunIfReady()
  }

  def started(node: Node): Unit = {
    stateChange(node, NodeState.STARTED)
    ready(node)
  }

  def running(node: Node): Unit = {
    stateChange(node, NodeState.RUNNING)
  }

  final def receive: PartialFunction[Any, Unit] = {
    case NWatchdog(millis) =>
      trace("WATCHDOG", s"$millis", TraceEvent.WATCHDOG)
      val ns = Duration.between(lastMessage, Instant.now()).toMillis
      if (ns > millis) {
        watchdog(millis)
      }

    case NRunIfReady() =>
      lastMessage = Instant.now()
      trace("RUN", "", TraceEvent.GMESSAGES)
      for (node <- graph.nodes) {
        initialize(node)
        if (node.parent.isEmpty) {
          topLevelNodes += node
        }
      }

    case NNode(node,actor) =>
      lastMessage = Instant.now()
      trace("NODE", s"$node", TraceEvent.GMESSAGES)
      actors.put(node, actor)
      allNodes += node

    case NInitialized(node: Node) =>
      lastMessage = Instant.now()
      initialized(node)

    case NStarted(node: Node) =>
      lastMessage = Instant.now()
      started(node)

    case NReady(node: Node) =>
      lastMessage = Instant.now()
      ready(node)

    case NRunning(node: Node) =>
      lastMessage = Instant.now()
      trace("RUNNING", s"$node", TraceEvent.GMESSAGES)
      running(node)

    case NStopped(node: Node) =>
      lastMessage = Instant.now()
      trace("STOPPED", s"$node", TraceEvent.GMESSAGES)
      stopped(node)

    case NFinished(node: Node) =>
      lastMessage = Instant.now()
      trace("FINISHED", s"$node", TraceEvent.GMESSAGES)
      finished(node)

    case NException(node, ex) =>
      crashAndBurn(ex)

    case NAbortExecution() =>
      stop()

    case NWatchdogTimeout() =>
      crashAndBurn(JafplException.watchdogTimeout())

    case m: Any =>
      lastMessage = Instant.now()
      trace("ERROR", s"$m", TraceEvent.GMESSAGES)
      log.error(s"UNEXPECT $m")
  }
}
