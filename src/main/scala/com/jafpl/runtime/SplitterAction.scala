package com.jafpl.runtime

import com.jafpl.graph.AtomicNode
import com.jafpl.messages.Message

class SplitterAction(override val node: AtomicNode) extends AbstractAction(node) {
  override def receive(port: String, message: Message): Unit = {
    super.receive(port, message)
    for (port <- node.outputs) {
      scheduler.receive(this, port, message)
    }
  }

  override def run(): Unit = {
    super.run()
    scheduler.finish(node)
  }
}
