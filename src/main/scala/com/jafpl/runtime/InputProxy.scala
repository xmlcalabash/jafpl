package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.Node
import com.jafpl.messages.{Message, Metadata}
import com.jafpl.runtime.GraphMonitor.GClose
import com.jafpl.steps.DataConsumer
import com.jafpl.util.PipelineMessage

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class InputProxy(private val monitor: ActorRef,
                 private val runtime: GraphRuntime,
                 private val node: Node) extends DataConsumer {
  var _closed = false
  val _items = mutable.ListBuffer.empty[Message]

  def closed: Boolean = _closed
  private[runtime] def items: ListBuffer[Message] = _items
  private[runtime] def clear() = {
    _items.clear()
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
