package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.ContainerEnd

private[runtime] class ForEachEndActor(private val monitor: ActorRef,
                                       private val runtime: GraphRuntime,
                                       private val node: ContainerEnd) extends EndActor(monitor, runtime, node)  {
  override protected def close(port: String): Unit = {
    openInputs -= port
    checkFinished()
    // Don't actually close the port...we're not done yet
  }
}
