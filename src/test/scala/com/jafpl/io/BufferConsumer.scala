package com.jafpl.io

import scala.collection.mutable.ListBuffer

class BufferConsumer() extends DevNullConsumer {
  val _items: ListBuffer[Any] = ListBuffer.empty[Any]

  def items: List[Any] = _items.toList

  override def receive(port: String, item: Any): Unit = {
    super.receive(port, item)
    _items += item
  }

  def dumpBuffers(): Unit = {
    for (item <- items) {
      println("Buf: " + item)
    }
  }
}
