package com.jafpl.io

import com.jafpl.messages.Metadata
import com.jafpl.steps.DataProvider

class PrintingConsumer extends DataProvider {
  override def send(item: Any, metadata: Metadata): Unit = {
    println("#none: " + item)
  }

  override def close(): Unit = Unit
}
