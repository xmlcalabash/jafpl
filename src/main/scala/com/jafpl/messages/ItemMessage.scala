package com.jafpl.messages

/** A item that flows between two ports.
  *
  * @param item The underlying item.
  */
class ItemMessage(val item: Any, val metadata: Metadata) extends Message {
  override def toString: String = {
    item.toString()
  }
}
