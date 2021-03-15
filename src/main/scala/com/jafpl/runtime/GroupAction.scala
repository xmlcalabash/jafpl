package com.jafpl.runtime

import com.jafpl.graph.ContainerStart

class GroupAction(override val node: ContainerStart) extends ContainerAction(node) {
  override def run(): Unit = {
    super.run()
    startChildren()
    scheduler.finish(node)
  }
}
