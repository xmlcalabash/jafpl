package com.jafpl.runtime

import com.jafpl.graph.ContainerStart
import com.jafpl.runtime.NodeState.NodeState
import org.slf4j.LoggerFactory

abstract class ContainerAction(override val node: ContainerStart) extends AbstractAction(node) {
  protected val logger = LoggerFactory.getLogger(this.getClass)

  def startChildren(): Unit = {
    for (child <- node.children) {
      scheduler.startNode(child)
    }
    scheduler.startNode(node.containerEnd)
  }

  def skipChildren(): Unit = {
    for (child <- node.children) {
      scheduler.stop(child)
    }
    scheduler.startNode(node.containerEnd)
  }

  override def reset(state: NodeState): Unit = {
    for (child <- node.children) {
      // No matter what state we've been put in, our children are in limbo until we run
      scheduler.reset(child, NodeState.LIMBO)
    }
    scheduler.reset(node.containerEnd, state)
  }
}
