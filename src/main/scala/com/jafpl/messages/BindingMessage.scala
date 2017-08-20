package com.jafpl.messages

private[jafpl] class BindingMessage(val name: String, val item: Any) {
  override def toString: String = {
    "${name=" + name + "=" + item + "}"
  }
}
