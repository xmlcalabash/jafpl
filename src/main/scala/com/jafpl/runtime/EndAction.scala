package com.jafpl.runtime

import com.jafpl.graph.ContainerEnd
import com.jafpl.messages.Message
import com.jafpl.util.TraceEventManager

class EndAction(override val node: ContainerEnd) extends AbstractAction(node) {
  override def run(): Unit = {
    super.run()
    for (port <- receivedPorts) {
      for (message <- received(port)) {
        scheduler.receive(node, port, message)
      }
    }
    scheduler.finish(node)
    cleanup()
  }
}
