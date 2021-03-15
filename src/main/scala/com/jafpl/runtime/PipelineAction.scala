package com.jafpl.runtime

import com.jafpl.graph.PipelineStart
import com.jafpl.messages.Message

class PipelineAction(override val node: PipelineStart) extends ContainerAction(node) {
  override def run(): Unit = {
    super.run()
    startChildren()
    scheduler.finish(node)
  }

  override def receive(port: String, message: Message): Unit = {
    scheduler.receive(node, port, message)
  }
}
