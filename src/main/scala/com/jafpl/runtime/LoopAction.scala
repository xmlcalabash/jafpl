package com.jafpl.runtime

import com.jafpl.graph.LoopStart
import com.jafpl.runtime.NodeState.NodeState

abstract class LoopAction(override val node: LoopStart) extends ContainerAction(node) {
  def finished(): Boolean = {
    node.iterationPosition >= node.iterationSize
  }

  override def startChildren(): Unit = {
    node.ranAtLeastOnce = true
    super.startChildren()
  }

  override def reset(state: NodeState): Unit = {
    super.reset(state)
    node.ranAtLeastOnce = false
  }
}
