package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{ContainerStart, Node, NodeState}
import com.jafpl.runtime.NodeActor.{NAbort, NAborted, NChkReady, NFinished, NReset, NResetted, NRun, NStart, NStarted, NStop, NStopped}

private[runtime] class StartActor(private val monitor: ActorRef,
                                  override protected val runtime: GraphRuntime,
                                  override protected val node: ContainerStart) extends NodeActor(monitor, runtime, node) {
  logEvent = TraceEvent.START

  override protected def start(): Unit = {
    for (child <- node.children) {
      stateChange(child, NodeState.STARTING)
      actors(child) ! NStart()
    }
  }

  override protected def started(child: Node): Unit = {
    stateChange(child, NodeState.STARTED)
    actors(child) ! NChkReady()
    var ready = true
    for (cnode <- node.children) {
      ready = ready && cnode.state == NodeState.STARTED || cnode.state == NodeState.READY
    }
    if (ready) {
      parent ! NStarted(node)
    } else {
      trace("¬STARTED", s"${nodeState(node)}", TraceEvent.STATECHANGE)
    }
  }

  override protected def ready(child: Node): Unit = {
    if (child.state == NodeState.STARTED || child.state == NodeState.RESET) {
      stateChange(child, NodeState.READY)
      if (node.state == NodeState.RUNNING) {
        trace("SETRUN", s"1: ${nodeState(child)}", TraceEvent.NMESSAGES)
        stateChange(child, NodeState.RUNNING)
        actors(child) ! NRun()
      }
    }
  }

  override protected def close(port: String): Unit = {
    if (openInputs.contains(port)) {
      super.close(port)
    } else {
      if (openOutputs.contains(port)) {
        trace("CLOSEO", s"$node: $port", TraceEvent.NMESSAGES)
        sendClose(port)
      } else {
        trace("CLOSE?", s"$node: $port", TraceEvent.NMESSAGES)
      }
    }
  }

  override protected def run(): Unit = {
    for (cnode <- node.children) {
      cnode.state match {
        case NodeState.READY =>
          trace("SETRUN", s"2: ${nodeState(cnode)}", TraceEvent.NMESSAGES)
          stateChange(cnode, NodeState.RUNNING)
          actors(cnode) ! NRun()
        case NodeState.RESET =>
          actors(cnode) ! NChkReady()
        case _ => ()
      }
    }
  }

  override protected def reset(): Unit = {
    inputBuffer.reset()
    outputBuffer.reset()
    receivedBindings.clear()
    configurePorts()
    for (child <- node.children) {
      stateChange(child, NodeState.RESETTING)
      actors(child) ! NReset()
    }
  }

  override protected def resetted(child: Node): Unit = {
    stateChange(child, NodeState.RESET)
    var reset = true
    for (cnode <- node.children) {
      reset = reset && cnode.state == NodeState.RESET
    }
    if (reset) {
      parent ! NResetted(node)
    } else {
      trace("UNRESET", s"${nodeState(node)}", TraceEvent.STATECHANGE)
    }
  }

  override protected def stop(): Unit = {
    for (child <- node.children) {
      stateChange(child, NodeState.STOPPING)
      actors(child) ! NStop()
    }
  }

  override protected def stopped(child: Node): Unit = {
    stateChange(child, NodeState.STOPPED)
    var stopped = true
    for (cnode <- node.children) {
      stopped = stopped && cnode.state == NodeState.STOPPED
    }
    if (stopped) {
      parent ! NStopped(node)
    }
  }

  override protected def abort(): Unit = {
    for (child <- node.children) {
      if (child.state != NodeState.ABORTED) {
        stateChange(child, NodeState.ABORTING)
      }
      actors(child) ! NAbort()
    }
  }

  override protected def aborted(child: Node): Unit = {
    stateChange(child, NodeState.ABORTED)
    var aborted = true
    for (cnode <- node.children) {
      aborted = aborted && cnode.state == NodeState.ABORTED
    }
    if (aborted) {
      parent ! NAborted(node)
    }
  }

  override protected def finished(child: Node): Unit = {
    stateChange(child, NodeState.FINISHED)
    var finished = true
    for (cnode <- node.children) {
      finished = finished && cnode.state == NodeState.FINISHED
    }
    if (finished) {
      parent ! NFinished(node)
    } else {
      trace("UNFINISH", s"${nodeState(node)}", TraceEvent.STATECHANGE)
    }
  }
}

