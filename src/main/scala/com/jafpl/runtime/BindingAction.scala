package com.jafpl.runtime

import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{Binding, Node}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class BindingAction(override val node: Binding) extends AbstractAction(node) {
  private val exprContext = ListBuffer.empty[Message]
  private val bindings = mutable.HashMap.empty[String, Message]

  override def initialize(scheduler: Scheduler, node: Node): Unit = {
    super.initialize(scheduler, node)
  }

  override def receive(port: String, item: Message): Unit = {
    super.receive(port, item)
    item match {
      case item: ItemMessage =>
        assert(port == "source")
        exprContext += item
      case binding: BindingMessage =>
        assert(port == "#bindings")
        bindings.put(binding.name, binding.message)
      case _ =>
        throw JafplException.unexpectedMessage(item.toString, port, node.location)
    }
  }

  override def run(): Unit = {
    super.run()

    // Pass any statics in as normal bindings
    for ((binding,message) <- node.staticBindings) {
      if (!bindings.contains(binding.name)) {
        bindings.put(binding.name, message)
      }
    }

    val expreval = scheduler.runtime.runtime.expressionEvaluator.newInstance()
    val answer = expreval.value(node.expression, exprContext.toList, bindings.toMap, node.params)

    val msg = new BindingMessage(node.name, answer)
    scheduler.receive(node, "result", msg)

    scheduler.finish(node)
  }
}
