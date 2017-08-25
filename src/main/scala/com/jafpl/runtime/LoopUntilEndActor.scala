package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.ContainerEnd
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GException, GLoop}

private[runtime] class LoopUntilEndActor(private val monitor: ActorRef,
                                         private val runtime: GraphRuntime,
                                         private val node: ContainerEnd) extends EndActor(monitor, runtime, node)  {
  override protected def input(port: String, msg: Message): Unit = {
    // An loop sends it's output back to the start.
    msg match {
      case message: ItemMessage =>
        monitor ! GLoop(node.start.get, message)
      case _ =>
        monitor ! GException(None,
          new PipelineException("badmessage", s"Unexpected message on $msg on $port", node.location))
        return
    }
  }


  override protected def close(port: String): Unit = {
    openInputs -= port
    checkFinished()
    // Don't actually close the port...we're not done yet
  }
}
