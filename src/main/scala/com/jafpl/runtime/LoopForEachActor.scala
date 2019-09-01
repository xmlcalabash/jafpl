package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{LoopEachStart, Node, NodeState}
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.NodeActor.{NFinished, NReset, NRun}

import scala.collection.mutable.ListBuffer

private[runtime] class LoopForEachActor(private val monitor: ActorRef,
                                        override protected val runtime: GraphRuntime,
                                        override protected val node: LoopEachStart)
  extends LoopActor(monitor, runtime, node) {

  private val queue = ListBuffer.empty[ItemMessage]
  logEvent = TraceEvent.LOOPEACH
  node.iterationPosition = 0L
  node.iterationSize = 0L

  override protected def input(port: String, message: Message): Unit = {
    if (port == "source") {
      message match {
        case item: ItemMessage =>
          queue += item
        case _ =>
          throw JafplException.unexpectedMessage(message.toString, port, node.location)
      }
    } else {
      super.input(port, message)
    }
  }

  override protected def run(): Unit = {
    if (node.iterationPosition == 0) {
      node.iterationSize = queue.size
    }

    if (queue.nonEmpty) {
      node.iterationPosition += 1
      val item = queue.head
      queue -= item
      sendMessage("current", item)
      sendClose("current")
      for (cnode <- node.children) {
        if (cnode.state == NodeState.READY) {
          trace("SETRUN", s"5: ${nodeState(cnode)}", TraceEvent.NMESSAGES)
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
      if (queue.isEmpty) {
        closeOutputs()
        parent ! NFinished(node)
      } else {
        stateChange(node, NodeState.LOOPING)
        for (child <- node.children) {
          actors(child) ! NReset()
        }
      }
    } else {
      trace("UNFINISH", s"${nodeState(node)}", TraceEvent.STATECHANGE)
    }
  }
}
