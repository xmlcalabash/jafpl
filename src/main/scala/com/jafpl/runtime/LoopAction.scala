package com.jafpl.runtime

import com.jafpl.graph.LoopStart

abstract class LoopAction(override val node: LoopStart) extends ContainerAction(node) {
  def finished(): Boolean = {
    node.iterationPosition >= node.iterationSize
  }
}
