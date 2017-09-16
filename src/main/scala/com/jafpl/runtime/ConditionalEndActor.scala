package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{ContainerEnd, Node}
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class ConditionalEndActor(private val monitor: ActorRef,
                                         private val runtime: GraphRuntime,
                                         private val node: ContainerEnd) extends EndActor(monitor, runtime, node)  {
  val buffer = mutable.HashMap.empty[String, ListBuffer[Message]]

  override protected def reset(): Unit = {
    super.reset()
    buffer.clear()
  }

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    // Buffer everything in case it all goes bang
    if (!buffer.contains(port)) {
      buffer.put(port, ListBuffer.empty[Message])
    }
    buffer(port) += item
  }

  override protected def close(port: String): Unit = {
    openInputs -= port
    checkFinished()
  }

  override protected[runtime] def checkFinished(): Unit = {
    trace(s"FINIFRDY ${node.start.get}/end ready:$readyToRun inputs:${openInputs.isEmpty} children:${unfinishedChildren.isEmpty}", "StepFinished")
    for (child <- unfinishedChildren) {
      trace(s"........ $child", "StepFinished")
    }
    if (readyToRun) {
      if (openInputs.isEmpty && unfinishedChildren.isEmpty) {
        for (port <- buffer.keySet) {
          for (item <- buffer(port)) {
            monitor ! GOutput(node.start.get, port, item)
          }
        }
        for (input <- node.inputs) {
          monitor ! GClose(node.start.get, input)
        }
        monitor ! GFinished(node)
      }
    }
  }
}
