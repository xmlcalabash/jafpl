package com.jafpl.messages

private[jafpl] class BindingMessage(val name: String, val item: Any) extends Message {
  override def toString: String = {
    "${name=" + name + "=" + item + "}"
  }
}
