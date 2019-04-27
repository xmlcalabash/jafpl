package com.jafpl.io

import com.jafpl.messages.{ItemMessage, Message, Metadata}
import com.jafpl.steps.DataConsumer
import com.jafpl.util.UniqueId

import scala.collection.mutable

class BufferConsumer extends DataConsumer {
  private val _items = mutable.ListBuffer.empty[Any]
  private val _metas = mutable.ListBuffer.empty[Metadata]
  private val _id = UniqueId.nextId.toString

  def items: List[Any] = _items.toList
  def metas: List[Any] = _items.toList

  override def receive(port: String, message: Message): Unit = {
    message match {
      case item: ItemMessage =>
        _items += item.item
        _metas += item.metadata
      case _ => Unit
    }
  }
}
