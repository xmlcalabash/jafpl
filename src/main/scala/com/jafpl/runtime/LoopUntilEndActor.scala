package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{ContainerEnd, Node}
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GException, GLoop, GOutput}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class LoopUntilEndActor(private val monitor: ActorRef,
                                         private val runtime: GraphRuntime,
                                         private val node: ContainerEnd) extends LoopEndActor(monitor, runtime, node)  {
  val buffer = mutable.HashMap.empty[String, ListBuffer[Message]]

  override protected def reset(): Unit = {
    // We got reset, so we're going around again.
    // That means the output we buffered on this loop is good.
    for (port <- buffer.keySet) {
      for (item <- buffer(port)) {
        node.start.get.outputCardinalities.put(port, node.start.get.outputCardinalities.getOrElse(port, 0L) + 1)
        monitor ! GOutput(node.start.get, port, item)
      }
    }
    buffer.clear()

    unfinishedChildren.clear()
    for (child <- node.start.get.children) {
      unfinishedChildren.add(child)
    }
    readyToRun = true
  }

  override protected def input(from: Node, fromPort: String, port: String, msg: Message): Unit = {
    if (port == "test") {
      // A loop sends it's output back to the start.
      msg match {
        case message: ItemMessage =>
          finished = false
          monitor ! GLoop(node.start.get, message)
        case _ =>
          monitor ! GException(None,
            JafplException.unexpectedMessage(msg.toString, port, node.location))
      }
    } else {
      // Buffer everything in case this iteration is false
      if (!buffer.contains(port)) {
        buffer.put(port, ListBuffer.empty[Message])
      }
      buffer(port) += msg
    }
  }
}
