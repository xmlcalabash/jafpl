package com.jafpl.runtime

import com.jafpl.graph.CatchStart
import com.jafpl.messages.ExceptionMessage

class CatchAction(override val node: CatchStart) extends ContainerAction(node) {
  override def run(): Unit = {
    super.run()
    startChildren()

    if (node.cause.isDefined) {
      scheduler.receive(node, "error", new ExceptionMessage(node.cause.get))
    }

    scheduler.finish(node)
  }
}
