package com.jafpl.runtime

import com.jafpl.graph.{ChooseStart, WhenStart}

class ChooseAction(override val node: ChooseStart) extends ContainerAction(node) {
  override def run(): Unit = {
    super.run()
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
