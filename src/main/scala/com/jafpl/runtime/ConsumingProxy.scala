package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.Node
import com.jafpl.runtime.GraphMonitor.GOutput
import com.jafpl.steps.Consumer

private[runtime] class ConsumingProxy(private val monitor: ActorRef,
                                    private val runtime: GraphRuntime,
                                    private val node: Node) extends Consumer {
  override def send(port: String, item: Any): Unit = {
    monitor ! GOutput(node, port, item)
  }

}
