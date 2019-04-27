package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.Node
import com.jafpl.messages.{ItemMessage, Message, Metadata, PipelineMessage}
import com.jafpl.runtime.GraphMonitor.{GException, GOutput}
import com.jafpl.steps.DataConsumer

import scala.collection.mutable

private[runtime] class ConsumingProxy(private val monitor: ActorRef,
                                      private val runtime: GraphRuntime,
                                      private val node: Node) extends DataConsumer {
  override def receive(port: String, message: Message): Unit = {
    node.outputCardinalities.put(port, node.outputCardinalities.getOrElse(port, 0L) + 1)
    monitor ! GOutput(node, port, message)
  }
}
