package com.jafpl.runtime

import com.jafpl.graph.GraphInput
import com.jafpl.messages.Message

class InputAction(override val node: GraphInput) extends AbstractAction(node) {
  override def receive(port: String, message: Message): Unit = {
    super.receive(port, message)
    scheduler.receiveOutput(node.name, message)
  }

  override def run(): Unit = {
    super.run()
    scheduler.finish(node)
  }
}
