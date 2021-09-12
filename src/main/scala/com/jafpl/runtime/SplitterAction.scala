package com.jafpl.runtime

import com.jafpl.graph.AtomicNode
import com.jafpl.messages.Message

import scala.collection.mutable.ListBuffer

class SplitterAction(override val node: AtomicNode) extends AbstractAction(node) {
  private val messages = ListBuffer.empty[Message]

  override def receive(port: String, message: Message): Unit = {
    messages += message
  }

  override def run(): Unit = {
    super.run()
    for (message <- messages) {
      for (port <- node.outputs) {
        scheduler.receive(this, port, message)
      }
    }
    scheduler.finish(node)
    cleanup()
  }

  override def cleanup(): Unit = {
    super.cleanup()
    messages.clear()
  }
}
