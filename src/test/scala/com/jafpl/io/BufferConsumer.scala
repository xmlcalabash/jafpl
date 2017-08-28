package com.jafpl.io

import com.jafpl.messages.Metadata
import com.jafpl.steps.DataProvider

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class BufferConsumer extends DataProvider {
  val _items: mutable.HashMap[Any, Metadata] = mutable.HashMap.empty[Any, Metadata]

  def items: List[Any] = _items.keySet.toList

  def metadata(item: Any): Metadata = {
    _items.getOrElse(item, Metadata.BLANK)
  }

  override def send(item: Any, metadata: Metadata): Unit = {
    _items.put(item, metadata)
  }

  override def close(): Unit = Unit
}
