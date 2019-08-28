package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{LoopStart, Node, NodeState}
import com.jafpl.runtime.NodeActor.NResetted

private[runtime] class LoopActor(private val monitor: ActorRef,
                                 override protected val runtime: GraphRuntime,
                                 override protected val node: LoopStart)
  extends StartActor(monitor, runtime, node)  {

  protected var running = false

  override protected def configurePorts(): Unit = {
    super.configurePorts()
    openOutputs -= "current" // this one doesn't count
  }

  override protected def close(port: String): Unit = {
    if (openInputs.contains(port)) {
      super.close(port)
    } else {
      // nop; don't close loop outputs until we're done looping
    }
  }

  override protected def reset(): Unit = {
    node.iterationPosition = 0L
    node.iterationSize = 0L
    super.reset()
  }

  override protected def resetted(child: Node): Unit = {
    stateChange(child, NodeState.RESET)
    var reset = true
    for (cnode <- node.children) {
      // Buffers can go from RESET to READY immediately
      reset = reset && (cnode.state == NodeState.RESET || cnode.state == NodeState.READY)
    }
    if (reset) {
      if (node.state == NodeState.LOOPING) {
        stateChange(node, NodeState.RUNNING)
        run()
      } else {
        parent ! NResetted(node)
      }
    } else {
      trace("UNRESET", s"${nodeState(node)}", TraceEvent.STATECHANGE)
    }
  }
}
