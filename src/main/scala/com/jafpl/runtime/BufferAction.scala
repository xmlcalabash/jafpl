package com.jafpl.runtime

import com.jafpl.graph.Buffer
import com.jafpl.messages.Message

import scala.collection.mutable.ListBuffer

class BufferAction(override val node: Buffer) extends AbstractAction(node) {
  private val buffer = ListBuffer.empty[Message]

  override def receive(port: String, message: Message): Unit = {
    super.receive(port, message)
    buffer += message
  }

  override def run(): Unit = {
    super.run()
    for (message <- buffer) {
      scheduler.receive(this, "result", message)
    }
    scheduler.finish(node)
  }

  override def abort(): Unit = {
    super.abort()
    buffer.clear()
  }
}
