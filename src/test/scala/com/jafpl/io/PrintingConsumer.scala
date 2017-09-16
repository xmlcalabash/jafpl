package com.jafpl.io

import com.jafpl.messages.{Message, Metadata}
import com.jafpl.steps.DataConsumer
import com.jafpl.util.UniqueId

class PrintingConsumer extends DataConsumer {
  private val _id = UniqueId.nextId.toString

  override def id: String = _id
  override def receive(port: String, message: Message): Unit = {
    println("#none: " + message)
  }
}
