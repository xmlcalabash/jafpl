package com.jafpl.runtime

import com.jafpl.graph.GraphOutput
import com.jafpl.messages.Message

class OutputAction(override val node: GraphOutput) extends AbstractAction(node) {
  override def receive(port: String, message: Message): Unit = {
    super.receive(port, message)
    scheduler.receiveOutput(node.name, message)
  }

  override def run(): Unit = {
    super.run()
    scheduler.finish(node)
  }
}
