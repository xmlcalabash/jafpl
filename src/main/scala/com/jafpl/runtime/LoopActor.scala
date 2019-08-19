package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{ContainerStart, Node}
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphMonitor.{GClose, GReset}

private[runtime] class LoopActor(private val monitor: ActorRef,
                                 override protected val runtime: GraphRuntime,
                                 override protected val node: ContainerStart)
  extends StartActor(monitor, runtime, node)  {

  protected var running = false

  override protected def configureOpenPorts(): Unit = {
    super.configureOpenPorts()
    openOutputs -= "current" // this one doesn't count
  }

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", logEvent)
    if (port == "source") {
      consume(port, item)
    } else {
      super.input(from, fromPort, port, item)
    }
  }

  override protected def close(port: String): Unit = {
    if (port == "source") {
      openInputs -= port
      // The source port on loops is magic; it's not connected to anything
      // else so closing it doesn't work.
      runIfReady()
    } else {
      // nop; don't close outputs until loop ends
      // super.close(port)
    }
  }

  override protected def finishIfReady(): Unit = {
    trace("FINRDY", s"$node children: $childrenHaveFinished  outputs: n/a", logEvent)
    if (childrenHaveFinished) {
      finished()
    }
  }

  override protected def resetFinished(child: Node): Unit = {
    if (childState.contains(child)) {
      childState(child) = NodeState.RESET
      trace("LOOPRSET", s"$node/$child $childState", logEvent)
      if (node.state == NodeState.RESTARTING) {
        runIfReady()
      } else {
        resetIfReady()
      }
    } else {
      throw new RuntimeException(s"Illegal state: $child is not a child of $node")
    }
  }

  protected[runtime] def restartLoop(): Unit = {
    trace("RSTRTLOOP", s"$node", logEvent)
    node.state = NodeState.RESTARTING

    running = false
    started = true
    threwException = false

    for (output <- node.outputs) {
      openOutputs.add(output)
    }
    if (openOutputs.contains("current")) {
      openOutputs -= "current" // this one doesn't count
    }

    for (child <- node.children) {
      childState(child) = NodeState.RESETTING
      child.state = NodeState.RESETTING
      monitor ! GReset(child)
    }

    runIfReady()
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [LoopFor]"
  }
}
