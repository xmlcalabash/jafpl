package com.jafpl.io

import com.jafpl.messages.Metadata
import com.jafpl.steps.DataConsumer

class PrintingConsumer extends DataConsumer {
  override def receive(port: String, item: Any, metadata: Metadata): Unit = {
    println("#none: " + item)
  }
}
