package com.jafpl.runtime

import com.jafpl.graph.AtomicNode
import com.jafpl.messages.Message

class SplitterAction(override val node: AtomicNode) extends AbstractAction(node) {
  override def run(): Unit = {
    super.run()
    for (message <- received("source")) {
      for (port <- node.outputs) {
        scheduler.receive(this, port, message)
      }
    }
    scheduler.finish(node)
    cleanup()
  }
}
