package com.jafpl.primitive

import com.jafpl.injection.PortInjectable
import com.jafpl.messages.{BindingMessage, ItemMessage}

class PrimitivePortInjectable(val port: String) extends PortInjectable {
  override def receiveBinding(message: BindingMessage): Unit = {
    println(s"PrimitivePortInjectable for $port received binding for ${message.name}")
  }

  override def run(context: ItemMessage): Unit = {
    println(s"PrimitivePortInjectable for $port ran $context")
  }

}
