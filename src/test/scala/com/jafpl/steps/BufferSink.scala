package com.jafpl.steps

import com.jafpl.messages.{ItemMessage, Message, Metadata}
import com.jafpl.util.UniqueId

import scala.collection.mutable.ListBuffer

class BufferSink() extends DefaultStep {
  private val _id = UniqueId.nextId.toString
  private val _items: ListBuffer[Any] = ListBuffer.empty[Any]

  def items: List[Any] = _items.toList

  override def id = _id
  override def receive(port: String, message: Message): Unit = {
    super.receive(port, message)
    message match {
      case item: ItemMessage =>
        _items += item.item
      case _ => Unit
    }
  }

  def dumpBuffers(): Unit = {
    for (item <- items) {
      println("Buf: " + item)
    }
  }
}
