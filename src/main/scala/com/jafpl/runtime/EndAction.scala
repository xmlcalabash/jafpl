package com.jafpl.runtime

import com.jafpl.graph.ContainerEnd
import com.jafpl.messages.Message
import com.jafpl.util.TraceEventManager

class EndAction(override val node: ContainerEnd) extends AbstractAction(node) {
  override def receive(port: String, message: Message): Unit = {
    super.receive(port, message)
    tracer.trace(s"$node sends $message to $port", TraceEventManager.CHOOSE)
    scheduler.receive(node, port, message);
  }

  override def run(): Unit = {
    super.run()
    scheduler.finish(node)
  }
}
