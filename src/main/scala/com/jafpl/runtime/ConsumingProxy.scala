package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.Node
import com.jafpl.messages.{ItemMessage, Message, Metadata}
import com.jafpl.runtime.GraphMonitor.{GException, GOutput}
import com.jafpl.steps.DataConsumer
import com.jafpl.util.PipelineMessage

import scala.collection.mutable

private[runtime] class ConsumingProxy(private val monitor: ActorRef,
                                      private val runtime: GraphRuntime,
                                      private val node: Node) extends DataConsumer {
  protected val cardinalities = mutable.HashMap.empty[String, Long]

  override def receive(port: String, item: Any, metadata: Metadata): Unit = {
    val card = cardinalities.getOrElse(port, 0L) + 1L
    cardinalities.put(port, card)
    item match {
      case msg: ItemMessage =>
        monitor ! GOutput(node, port, msg)
      case msg: Message =>
        monitor ! GException(None, new PipelineException("badmessage", s"Unexpected message on send: $item", node.location))
      case _ =>
        monitor ! GOutput(node, port, new PipelineMessage(item, metadata))
    }
  }

  def reset(): Unit = {
    cardinalities.clear()
  }

  def cardinality(port: String): Long = {
    cardinalities.getOrElse(port, 0L)
  }
}
