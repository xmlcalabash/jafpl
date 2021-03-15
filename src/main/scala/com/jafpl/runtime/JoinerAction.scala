package com.jafpl.runtime

import com.jafpl.graph.{JoinMode, Joiner}
import com.jafpl.messages.Message
import com.jafpl.runtime.NodeState.NodeState

class JoinerAction(override val node: Joiner) extends AbstractAction(node) {
  private val joinBuffer = new IOBuffer()

  override def receive(port: String, message: Message): Unit = {
    super.receive(port, message)
    joinBuffer.consume(port, message)
  }

  override def run(): Unit = {
    super.run()

    if (node.mode == JoinMode.PRIORITY && joinBuffer.ports.contains("source_1")) {
      for (item <- joinBuffer.messages("source_1")) {
        scheduler.receive(node, "result", item)
      }
    } else {
      var portNum = 0
      while (joinBuffer.ports.nonEmpty) {
        portNum += 1
        val port = s"source_$portNum"
        if (joinBuffer.ports.contains(port)) {
          for (item <- joinBuffer.messages(port)) {
            scheduler.receive(node, "result", item)
          }
          joinBuffer.clear(port)
        }
      }
    }

    scheduler.finish(node)
  }

  override def reset(state: NodeState): Unit = {
    joinBuffer.reset()
    super.reset(state)
  }
}
