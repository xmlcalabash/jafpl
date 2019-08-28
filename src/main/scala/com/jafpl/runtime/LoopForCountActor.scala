package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{LoopForStart, Node, NodeState}
import com.jafpl.messages.{ItemMessage, Metadata}
import com.jafpl.runtime.NodeActor.{NFinished, NReset, NRun}

private[runtime] class LoopForCountActor(private val monitor: ActorRef,
                                          override protected val runtime: GraphRuntime,
                                          override protected val node: LoopForStart)
  extends LoopActor(monitor, runtime, node) {

  private var current = node.countFrom
  logEvent = TraceEvent.LOOPFOR
  node.iterationPosition = 0L
  node.iterationSize = 0L

  override protected def run(): Unit = {
    if (node.iterationSize == 0) {
      val diff = if (node.countFrom > node.countTo) {
        node.countFrom - node.countTo + 1
      } else {
        node.countTo - node.countFrom + 1
      }
      node.iterationSize = (diff + node.countBy.abs - 1) / node.countBy.abs
    }

    val initiallyTrue = if (node.countBy > 0) {
      current <= node.countTo
    } else {
      current >= node.countTo
    }

    if (initiallyTrue) {
      stateChange(node, NodeState.RUNNING)
      node.iterationPosition += 1
      sendMessage("current", new ItemMessage(current, Metadata.NUMBER))
      sendClose("current")
      for (cnode <- node.children) {
        if (cnode.state == NodeState.READY) {
          stateChange(cnode, NodeState.RUNNING)
          actors(cnode) ! NRun()
        }
      }
    } else {
      closeOutputs()
      parent ! NFinished(node)
    }
  }

  override protected def finished(child: Node): Unit = {
    stateChange(child, NodeState.FINISHED)
    var finished = true
    for (cnode <- node.children) {
      finished = finished && cnode.state == NodeState.FINISHED
    }
    if (finished) {
      current += node.countBy
      val loopAgain = if (node.countBy > 0) {
        current <= node.countTo
      } else {
        current >= node.countTo
      }
      if (loopAgain) {
        stateChange(node, NodeState.LOOPING)
        for (child <- node.children) {
          actors(child) ! NReset()
        }
      } else {
        closeOutputs()
        parent ! NFinished(node)
      }
    } else {
      trace("UNFINISH", s"${nodeState(node)}", TraceEvent.STATECHANGE)
    }
  }
}
