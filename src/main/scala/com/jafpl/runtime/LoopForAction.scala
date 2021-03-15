package com.jafpl.runtime

import com.jafpl.graph.LoopForStart
import com.jafpl.messages.{ItemMessage, Metadata}
import com.jafpl.runtime.NodeState.NodeState

class LoopForAction(override val node: LoopForStart) extends LoopAction(node) {
  private var current = node.countFrom
  private var looping = false

  override def run(): Unit = {
    super.run()

    if (!looping) {
      looping = true
      val diff = if (node.countFrom > node.countTo) {
        node.countFrom - node.countTo + 1
      } else {
        node.countTo - node.countFrom + 1
      }
      current = node.countFrom
      node.iterationPosition = 0
      node.iterationSize = (diff + node.countBy.abs - 1) / node.countBy.abs
    }

    val initiallyTrue = if (node.countBy > 0) {
      current <= node.countTo
    } else {
      current >= node.countTo
    }

    if (initiallyTrue) {
      scheduler.receive(node, "current", new ItemMessage(current, Metadata.NUMBER))
      node.iterationPosition += 1
      current += node.countBy
    }

    startChildren()
    scheduler.finish(node)
  }

  override def reset(state: NodeState): Unit = {
    super.reset(state)

    node.iterationPosition = 0
    node.iterationSize = 0
    current = 0
    looping = false
  }
}
