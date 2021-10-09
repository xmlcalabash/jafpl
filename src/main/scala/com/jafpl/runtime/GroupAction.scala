package com.jafpl.runtime

import com.jafpl.graph.ContainerStart

class GroupAction(override val node: ContainerStart) extends ContainerAction(node) {
  override def run(): Unit = {
    super.run()

    logger.info(s"Running group ${node.userLabel.getOrElse("")}")

    startChildren()
    scheduler.finish(node)
  }
}
