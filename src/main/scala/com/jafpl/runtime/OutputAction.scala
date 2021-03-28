package com.jafpl.runtime

import com.jafpl.graph.GraphOutput
import com.jafpl.messages.Message

class OutputAction(override val node: GraphOutput) extends AbstractAction(node) {
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
