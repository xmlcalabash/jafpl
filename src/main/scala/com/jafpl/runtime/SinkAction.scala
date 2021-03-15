package com.jafpl.runtime

import com.jafpl.graph.Sink
import com.jafpl.messages.Message

class SinkAction(override val node: Sink) extends AbstractAction(node) {
  override def receive(port: String, message: Message): Unit = {
    super.receive(port, message)
  }

  override def run(): Unit = {
    super.run()
    scheduler.finish(node)
  }
}
