package com.jafpl.runtime

import com.jafpl.graph.{ChooseStart, WhenStart}
import com.jafpl.runtime.AbstractAction.showRunningMessage

class ChooseAction(override val node: ChooseStart) extends ContainerAction(node) {
  override def run(): Unit = {
    super.run()
    if (showRunningMessage) {
      logger.info("Running choose {}", node.userLabel.getOrElse(""))
    }

    for (child <- node.children) {
      child match {
        case _: WhenStart => () // At most one already marked runnable by scheduler
        case _ => scheduler.startNode(child)
      }
    }
    scheduler.startNode(node.containerEnd)
    scheduler.finish(node)
  }
}
