package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{ContainerEnd, Node}
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.GFinishedViewport

import scala.collection.mutable.ListBuffer

private[runtime] class ViewportEndActor(private val monitor: ActorRef,
                                        private val runtime: GraphRuntime,
                                        private val node: ContainerEnd) extends EndActor(monitor, runtime, node)  {
  private val buffer = ListBuffer.empty[ItemMessage]

  override protected def reset(): Unit = {
    super.reset()
    buffer.clear()
  }

  override protected def input(from: Node, fromPort: String, port: String, msg: Message): Unit = {
    msg match {
      case item: ItemMessage =>
        buffer += item
      case _ => Unit
    }
  }

  override protected def close(port: String): Unit = {
    openInputs -= port
    checkFinished()
    // Don't actually close the port...we're not done yet
  }

  override protected[runtime] def checkFinished(): Unit = {
    trace(s"FINIFRDY ${node.start.get}/end ready:$readyToRun inputs:${openInputs.isEmpty} children:${unfinishedChildren.isEmpty}", "StepFinished")
    for (child <- unfinishedChildren) {
      trace(s"........ $child", "StepFinished")
    }
    if (readyToRun) {
      if (openInputs.isEmpty && unfinishedChildren.isEmpty) {
        monitor ! GFinishedViewport(node, buffer.toList)
      }
    }
  }
}
