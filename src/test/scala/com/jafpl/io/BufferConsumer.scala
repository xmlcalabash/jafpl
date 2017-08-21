package com.jafpl.io

import com.jafpl.steps.DataProvider

import scala.collection.mutable.ListBuffer

class BufferConsumer extends DataProvider {
  val _items: ListBuffer[Any] = ListBuffer.empty[Any]

  def items: List[Any] = _items.toList

  override def send(item: Any): Unit = {
    _items += item
  }

  override def close(): Unit = Unit
}
