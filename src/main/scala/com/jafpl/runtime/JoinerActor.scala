package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.Joiner
import com.jafpl.runtime.GraphMonitor.GOutput

private[runtime] class JoinerActor(private val monitor: ActorRef,
                                 private val runtime: GraphRuntime,
                                 private val node: Joiner) extends NodeActor(monitor, runtime, node)  {
  override protected def input(port: String, item: Any): Unit = {
    monitor ! GOutput(node, "result", item)
  }
}
