package com.jafpl.runtime

import com.jafpl.graph.Binding
import com.jafpl.messages.{BindingMessage, Message}

import scala.collection.mutable

class BindingAction(override val node: Binding) extends AbstractAction(node) {
  override def run(): Unit = {
    super.run()

    // Pass any statics in as normal bindings
    val bindings = mutable.HashMap.empty[String, Message]
    for (key <- receivedBindings.keys) {
      bindings.put(key, receivedBindings(key))
    }
    for ((binding,message) <- node.staticBindings) {
      if (!bindings.contains(binding.name)) {
        bindings.put(binding.name, message)
      }
    }

    if (scheduler.getBinding(node).isDefined) {
      val msg = new BindingMessage(node.name, scheduler.getBinding(node).get)
      scheduler.receive(node, "result", msg)
      scheduler.finish(node)
    } else {
      val expreval = scheduler.runtime.runtime.expressionEvaluator.newInstance()
      try {
        val answer = expreval.value(node.expression, received("source"), bindings.toMap, node.params)
        val msg = new BindingMessage(node.name, answer)
        scheduler.receive(node, "result", msg)
        scheduler.finish(node)
      } catch {
        case t: Throwable =>
          scheduler.reportException(node, t)
      }
    }
  }
}
