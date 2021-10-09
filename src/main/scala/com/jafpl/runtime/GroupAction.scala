package com.jafpl.runtime

import com.jafpl.graph.ContainerStart
import com.jafpl.runtime.AbstractAction.showRunningMessage

class GroupAction(override val node: ContainerStart) extends ContainerAction(node) {
  override def run(): Unit = {
    super.run()
    if (showRunningMessage) {
      logger.info("Running group {}", node.userLabel.getOrElse(""))
    }

    startChildren()
    scheduler.finish(node)
  }
}
