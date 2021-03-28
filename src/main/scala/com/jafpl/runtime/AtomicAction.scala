package com.jafpl.runtime

import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{AtomicNode, Node}
import com.jafpl.messages.BindingMessage

class AtomicAction(override val node: AtomicNode) extends AbstractAction(node) {
  override def initialize(scheduler: Scheduler, node: Node): Unit = {
    super.initialize(scheduler, node)
    if (node.step.isDefined) {
      node.step.get.setConsumer(this)
    }
  }

  override def run(): Unit = {
    super.run()

    if (node.step.isDefined) {
      for (key <- receivedBindings.keys) {
        node.step.get.receiveBinding(new BindingMessage(key, receivedBindings(key)))
      }
      for (port <- receivedPorts) {
        for (message <- received(port)) {
          node.step.get.consume(port, message)
        }
      }

      try {
        node.step.get.run()
      } catch {
        case t: Throwable =>
          scheduler.reportException(node, t)
          return
      }
    } else {
      scheduler.reportException(node, JafplException.noStepFor(node.toString, node.location))
      return
    }

    scheduler.finish(node)
    cleanup()
  }
}
