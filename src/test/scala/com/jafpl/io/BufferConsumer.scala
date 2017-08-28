package com.jafpl.io

import com.jafpl.messages.Metadata
import com.jafpl.steps.DataConsumer

import scala.collection.mutable

class BufferConsumer extends DataConsumer {
  val _items: mutable.HashMap[Any, Metadata] = mutable.HashMap.empty[Any, Metadata]

  def items: List[Any] = _items.keySet.toList

  def metadata(item: Any): Metadata = {
    _items.getOrElse(item, Metadata.BLANK)
  }

  override def receive(port: String, item: Any, metadata: Metadata): Unit = {
    _items.put(item, metadata)
  }
}
