package com.jafpl.runtime

import com.jafpl.graph.{CatchStart, FinallyStart, TryCatchStart}
import com.jafpl.runtime.AbstractAction.showRunningMessage

class TryCatchAction(override val node: TryCatchStart) extends ContainerAction(node) {
  override def run(): Unit = {
    super.run()
    if (showRunningMessage) {
      logger.info("Running try {}", node.userLabel.getOrElse(""))
    }

    for (child <- node.children) {
      child match {
        case _: CatchStart => () // Don't run these
        case _: FinallyStart => () // Don't run this either
        case _ => scheduler.startNode(child)
      }
    }
    scheduler.startNode(node.containerEnd)
    scheduler.finish(node)
  }
}
