package com.jafpl.io

import com.jafpl.messages.{Message, Metadata}
import com.jafpl.steps.DataConsumer

class PrintingConsumer extends DataConsumer {
  override def receive(port: String, message: Message): Unit = {
    println("#none: " + message)
  }
}
