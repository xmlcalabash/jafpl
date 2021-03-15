package com.jafpl.runtime

import com.jafpl.graph.{AtomicNode, Node}
import com.jafpl.messages.{BindingMessage, Message}

class AtomicAction(override val node: AtomicNode) extends AbstractAction(node) {
  override def initialize(scheduler: Scheduler, node: Node): Unit = {
    super.initialize(scheduler, node)
    node.step.get.setConsumer(this)
  }

  override def receive(port: String, message: Message): Unit = {
    super.receive(port, message)
    message match {
      case b: BindingMessage => node.step.get.receiveBinding(b)
      case _ => node.step.get.consume(port, message)
    }
  }

  override def run(): Unit = {
    super.run()
    try {
      node.step.get.run()
    } catch {
      case t: Throwable =>
        scheduler.reportException(node, t)
        return
    }
    scheduler.finish(node)
  }
}
