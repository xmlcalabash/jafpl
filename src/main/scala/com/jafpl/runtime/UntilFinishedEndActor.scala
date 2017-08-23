package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.ContainerEnd
import com.jafpl.runtime.GraphMonitor.GLoop

private[runtime] class UntilFinishedEndActor(private val monitor: ActorRef,
                                             private val runtime: GraphRuntime,
                                             private val node: ContainerEnd) extends EndActor(monitor, runtime, node)  {
  override protected def input(port: String, item: Any): Unit = {
    // A while loop sends it's output back to the start.
    monitor ! GLoop(node.start.get, item)
  }


  override protected def close(port: String): Unit = {
    openInputs -= port
    checkFinished()
    // Don't actually close the port...we're not done yet
  }
}
