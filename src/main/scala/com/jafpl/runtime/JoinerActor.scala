package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{Joiner, Node}
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphMonitor.GOutput

private[runtime] class JoinerActor(private val monitor: ActorRef,
                                 private val runtime: GraphRuntime,
                                 private val node: Joiner) extends NodeActor(monitor, runtime, node)  {
  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    monitor ! GOutput(node, "result", item)
  }
}
