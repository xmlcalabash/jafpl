package com.jafpl.runtime

import com.jafpl.exceptions.JafplException
import com.jafpl.graph.LoopForStart
import com.jafpl.messages.{ItemMessage, Metadata}
import com.jafpl.runtime.AbstractAction.showRunningMessage
import com.jafpl.runtime.NodeState.NodeState

class LoopForAction(override val node: LoopForStart) extends LoopAction(node) {
  private var current = node.countFrom
  private var looping = false

  override def run(): Unit = {
    super.run()
    if (showRunningMessage) {
      if (node.userLabel.isDefined) {
        logger.info("Running for loop {}: {} to {} by {}", node.userLabel.get, node.countFrom, node.countTo, node.countBy)
      } else {
        logger.info("Running for loop: {} to {} by {}", node.countFrom, node.countTo, node.countBy)
      }
    }

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

      if ((node.countFrom <= node.countTo && node.countBy <= 0)
        || (node.countFrom >= node.countTo && node.countBy >= 0)) {
        throw JafplException.invalidLoopBounds(node.countFrom, node.countTo, node.countBy)
      }
    }

    if (!finished()) {
      scheduler.receive(node, "current", new ItemMessage(current, Metadata.NUMBER))
      node.iterationPosition += 1
      current += node.countBy
    }

    startChildren()
    scheduler.finish(node)
  }

  override def finished(): Boolean = {
    if (node.countBy > 0) {
      current > node.countTo
    } else {
      current < node.countTo
    }
  }

  override def reset(state: NodeState): Unit = {
    super.reset(state)

    node.iterationPosition = 0
    node.iterationSize = 0
    current = 0
    looping = false
  }
}
