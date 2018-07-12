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
  protected val cardinalities = mutable.HashMap.empty[String, Long]

  override def id: String = node.id

  override def receive(port: String, message: Message): Unit = {
    val card = cardinalities.getOrElse(port, 0L) + 1L
    cardinalities.put(port, card)
    monitor ! GOutput(node, port, message)
  }

  def reset(): Unit = {
    cardinalities.clear()
  }

  def cardinality(port: String): Long = {
    cardinalities.getOrElse(port, 0L)
  }
}
