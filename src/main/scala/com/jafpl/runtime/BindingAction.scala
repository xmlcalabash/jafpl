package com.jafpl.runtime

import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{Binding, Node}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

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

    val expreval = scheduler.runtime.runtime.expressionEvaluator.newInstance()
    val answer = expreval.value(node.expression, received("source"), bindings.toMap, node.params)

    val msg = new BindingMessage(node.name, answer)
    scheduler.receive(node, "result", msg)

    scheduler.finish(node)
  }
}
