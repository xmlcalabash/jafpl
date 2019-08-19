package com.jafpl.io

import com.jafpl.messages.{Message, Metadata}
import com.jafpl.steps.DataConsumer
import com.jafpl.util.UniqueId

class PrintingConsumer extends DataConsumer {
  override def consume(port: String, message: Message): Unit = {
    println("#none: " + message)
  }
}
