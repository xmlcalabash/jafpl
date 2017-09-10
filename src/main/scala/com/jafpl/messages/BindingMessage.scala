package com.jafpl.messages

class BindingMessage(val name: String, val message: Message) extends Message {
  override def toString: String = {
    "${name=" + name + "=" + message + "}"
  }
}
