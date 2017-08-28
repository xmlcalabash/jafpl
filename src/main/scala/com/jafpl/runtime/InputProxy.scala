package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.Node
import com.jafpl.messages.{Message, Metadata}
import com.jafpl.steps.DataProvider
import com.jafpl.util.PipelineMessage

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class InputProxy(private val monitor: ActorRef,
                 private val runtime: GraphRuntime,
                 private val node: Node) extends DataProvider {
  var _closed = false
  val _items = mutable.ListBuffer.empty[Message]

  def closed: Boolean = _closed
  private[runtime] def items: ListBuffer[Message] = _items
  private[runtime] def clear() = {
    _items.clear()
  }

  def send(item: Any, metadata: Metadata): Unit = {
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
