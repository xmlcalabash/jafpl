package com.jafpl.steps

import scala.collection.mutable.ListBuffer

class BufferSink() extends DefaultStep {
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
