package com.jafpl.runtime

import com.jafpl.graph.GraphInput
import com.jafpl.messages.Message

class InputAction(override val node: GraphInput) extends AbstractAction(node) {
  override def run(): Unit = {
    super.run()
    for (port <- receivedPorts) {
      for (message <- received(port)) {
        scheduler.receiveOutput(node.name, message)
      }
    }
    scheduler.finish(node)
    cleanup()
  }
}
