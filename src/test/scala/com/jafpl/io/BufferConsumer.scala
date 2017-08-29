package com.jafpl.io

import com.jafpl.messages.Metadata
import com.jafpl.steps.DataConsumer

import scala.collection.mutable

class BufferConsumer extends DataConsumer {
  private val _items = mutable.ListBuffer.empty[Any]
  private val _metas = mutable.ListBuffer.empty[Metadata]

  def items: List[Any] = _items.toList
  def metas: List[Any] = _items.toList

  override def receive(port: String, item: Any, metadata: Metadata): Unit = {
    _items += item
    _metas += metadata
  }
}
