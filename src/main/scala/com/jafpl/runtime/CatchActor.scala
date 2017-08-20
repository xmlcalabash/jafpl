package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.ContainerStart
import com.jafpl.runtime.GraphMonitor.GStart

private[runtime] class CatchActor(private val monitor: ActorRef,
                                private val runtime: GraphRuntime,
                                private val node: ContainerStart) extends StartActor(monitor, runtime, node)  {
  protected[runtime] def start(cause: Throwable): Unit = {
    readyToRun = true

    for (child <- node.children) {
      trace(s"START ... $child (for $node)", "Run")
      monitor ! GStart(child)
    }
  }
}
