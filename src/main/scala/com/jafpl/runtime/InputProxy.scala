package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.Node
import com.jafpl.messages.{Message, Metadata}
import com.jafpl.steps.{DataConsumer, DataProvider}
import com.jafpl.util.PipelineMessage

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class InputProxy(private val monitor: ActorRef,
                 private val runtime: GraphRuntime,
                 private val node: Node) extends DataConsumer with DataProvider {
  private var _closed = false
  private val _items = mutable.ListBuffer.empty[Message]

  def closed: Boolean = _closed
  private[runtime] def items: ListBuffer[Message] = _items
  private[runtime] def clear(): Unit = {
    _items.clear()
  }

  override def send(item: Any, metadata: Metadata): Unit = {
    // In fact the port name is irrelevant in the input proxy case...which is the whole point of send()!
    receive("source", item, metadata)
  }

  def receive(port: String, item: Any, metadata: Metadata): Unit = {
    item match {
      case msg: Message =>
        _items += msg
      case _ =>
        _items += new PipelineMessage(item, metadata)
    }
  }

  def close(): Unit = {
    _closed = true
  }
}
