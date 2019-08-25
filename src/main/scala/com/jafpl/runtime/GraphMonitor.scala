package com.jafpl.runtime

import akka.actor.{ActorRef, PoisonPill}
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{ContainerStart, Graph, Node, NodeState}
import com.jafpl.runtime.NodeActor.{NAbortExecution, NException, NFinished, NInitialize, NInitialized, NNode, NReady, NRunIfReady, NRunning, NStart, NStarted, NStop, NStopped, NWatchdog, NWatchdogTimeout}

import scala.collection.mutable

private[runtime] class GraphMonitor(private val graph: Graph, override protected val runtime: GraphRuntime) extends TracingActor(runtime) {
  protected val unstoppedNodes = mutable.HashSet.empty[Node]
  protected val topLevelNodes = mutable.HashSet.empty[Node]
  private val actors = mutable.HashMap.empty[Node, ActorRef]
  private var exception: Option[Exception] = None
  protected var logEvent = TraceEvent.MONITOR

  def watchdog(millis: Long): Unit = {
    trace("WATCHDOG", s"$millis", TraceEvent.WATCHDOG)
    for (node <- unstoppedNodes) {
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
  }

  def stopPipeline(): Unit = {
    trace("STOPPIPE", "", logEvent)
    if (topLevelNodes.isEmpty) {
      poisonUnstoppedNodes()
      runtime.finish()
    } else {
      for (node <- topLevelNodes) {
        stateChange(node, NodeState.STOPPING)
        actors(node) ! NStop()
      }
    }
  }

  def stopped(node: Node): Unit = {
    stateChange(node, NodeState.STOPPED)
    topLevelNodes -= node
    trace("POISON", s"$node", TraceEvent.NMESSAGES)
    actors(node) ! PoisonPill
    unstoppedNodes -= node
    if (topLevelNodes.isEmpty) {
      poisonUnstoppedNodes()
      if (exception.isDefined) {
        runtime.finish(exception.get)
      } else {
        runtime.finish()
      }
    }
  }

  def poisonUnstoppedNodes(): Unit = {
    for (node <- unstoppedNodes) {
      trace("POISON", s"$node", TraceEvent.NMESSAGES)
      actors(node) ! PoisonPill
    }
    unstoppedNodes.clear()
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

    actors(node) ! NInitialize(pref, actors.toMap, pmap.toMap)
  }

  def initialized(node: Node): Unit = {
    stateChange(node, NodeState.INIT)
    var ready = true
    // unstoppedNodes == all nodes at the moment
    for (node <- unstoppedNodes) {
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
      if (runtime.lastMessageAge > millis) {
        watchdog(millis)
      }

    case NRunIfReady() =>
      runtime.noteMessageTime()
      trace("RUN", "", TraceEvent.GMESSAGES)
      for (node <- graph.nodes) {
        initialize(node)
        if (node.parent.isEmpty) {
          topLevelNodes += node
        }
      }

    case NNode(node,actor) =>
      runtime.noteMessageTime()
      trace("NODE", s"$node", TraceEvent.GMESSAGES)
      actors.put(node, actor)
      unstoppedNodes += node

    case NInitialized(node: Node) =>
      runtime.noteMessageTime()
      initialized(node)

    case NStarted(node: Node) =>
      runtime.noteMessageTime()
      started(node)

    case NReady(node: Node) =>
      runtime.noteMessageTime()
      ready(node)

    case NRunning(node: Node) =>
      runtime.noteMessageTime()
      trace("RUNNING", s"$node", TraceEvent.GMESSAGES)
      running(node)

    case NStopped(node: Node) =>
      runtime.noteMessageTime()
      trace("STOPPED", s"$node", TraceEvent.GMESSAGES)
      stopped(node)

    case NFinished(node: Node) =>
      runtime.noteMessageTime()
      trace("FINISHED", s"$node", TraceEvent.GMESSAGES)
      finished(node)

    case NException(node, ex) =>
      crashAndBurn(ex)

    case NAbortExecution() =>
      stop()

    case NWatchdogTimeout() =>
      crashAndBurn(JafplException.watchdogTimeout())

    case m: Any =>
      runtime.noteMessageTime()
      trace("ERROR", s"$m", TraceEvent.GMESSAGES)
      log.error(s"UNEXPECT $m")
  }
}
