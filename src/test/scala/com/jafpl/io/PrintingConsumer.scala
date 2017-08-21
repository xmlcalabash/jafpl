package com.jafpl.io

import com.jafpl.steps.DataProvider

class PrintingConsumer extends DataProvider {
  override def send(item: Any): Unit = {
    println("#none: " + item)
  }

  override def close(): Unit = Unit

  override def send(port: String, item: Any): Unit = {
    println(port + ": " + item)
  }
}
