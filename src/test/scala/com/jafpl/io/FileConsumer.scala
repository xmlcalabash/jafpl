package com.jafpl.io

class FileConsumer(filename: Option[String]) extends DevNullConsumer {
  // FIXME: Implement the file part.

  override def receive(port: String, item: Any): Unit = {
    super.receive(port, item)
  }
}
