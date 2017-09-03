package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.ContainerEnd
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GException, GLoop, GOutput}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class LoopForEndActor(private val monitor: ActorRef,
                                       private val runtime: GraphRuntime,
                                       private val node: ContainerEnd) extends EndActor(monitor, runtime, node)  {
  val buffer = mutable.HashMap.empty[String, ListBuffer[Message]]

  override protected def reset(): Unit = {
    // We got reset, so we're going around again.
    // That means the output we buffered on this loop is good.
    for (port <- buffer.keySet) {
      for (item <- buffer(port)) {
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

  override protected def input(port: String, msg: Message): Unit = {
    if (port == "test") {
      // A loop sends it's output back to the start.
      msg match {
        case message: ItemMessage =>
          monitor ! GLoop(node.start.get, message)
        case _ =>
          monitor ! GException(None,
            new PipelineException("badmessage", s"Unexpected message $msg on port $port", node.location))
      }
    } else {
      // Buffer everything in case this iteration was false
      if (!buffer.contains(port)) {
        buffer.put(port, ListBuffer.empty[Message])
      }
      buffer(port) += msg
    }
  }

  override protected def close(port: String): Unit = {
    openInputs -= port
    checkFinished()
    // Don't actually close the port...we're not done yet
  }
}
