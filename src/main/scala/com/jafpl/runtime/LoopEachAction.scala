package com.jafpl.runtime

import com.jafpl.graph.LoopEachStart
import com.jafpl.messages.Message
import com.jafpl.runtime.AbstractAction.showRunningMessage
import com.jafpl.runtime.NodeState.NodeState

import scala.collection.mutable.ListBuffer

class LoopEachAction(override val node: LoopEachStart) extends LoopAction(node) {
  protected val source: ListBuffer[Message] = ListBuffer.empty[Message]
  protected var ipos: Long = 0L

  override def receive(port: String, message: Message): Unit = {
    super.receive(port, message)
    if (port == "source") {
      source += message
    } else {
      scheduler.receive(node, port, message)
    }
  }

  override def run(): Unit = {
    super.run()
    if (showRunningMessage) {
      logger.info("Running for-each {}", node.userLabel.getOrElse(""))
    }

    if (source.nonEmpty) {
      if (ipos == 0) {
        node.iterationSize = source.length
      }
      node.iterationPosition = ipos + 1
      ipos += 1

      scheduler.receive(node, "current", source.remove(0))
      startChildren()
    } else {
      skipChildren()
    }

    scheduler.finish(node)
  }

  override def reset(state: NodeState): Unit = {
    super.reset(state)

    node.iterationSize = 0L
    node.iterationPosition = 0L
    ipos = 0
    source.clear()
  }
}
