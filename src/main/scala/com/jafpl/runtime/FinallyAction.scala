package com.jafpl.runtime

import com.jafpl.graph.FinallyStart
import com.jafpl.messages.ExceptionMessage

class FinallyAction(override val node: FinallyStart) extends ContainerAction(node) {
  override def run(): Unit = {
    super.run()
    startChildren()

    if (node.cause.isDefined) {
      scheduler.receive(node, "error", new ExceptionMessage(node.cause.get))
    }

    scheduler.finish(node)
  }
}
