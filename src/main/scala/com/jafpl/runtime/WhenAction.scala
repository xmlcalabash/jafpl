package com.jafpl.runtime

import com.jafpl.graph.WhenStart
import com.jafpl.messages.Message
import com.jafpl.runtime.NodeState.NodeState
import com.jafpl.util.TraceEventManager

import scala.collection.mutable.ListBuffer

class WhenAction(override val node: WhenStart) extends ContainerAction(node) {
  private val contextItem = ListBuffer.empty[Message]

  override def receive(port: String, message: Message): Unit = {
    if (port == "condition") {
      contextItem += message
      tracer.trace(s"WHEN  ${node} received context item: ${message}", TraceEventManager.CHOOSE)
    } else {
      throw new RuntimeException(s"Unexpected input port on ${node}: ${port}")
    }
  }

  def test: Boolean = {
    val eval = scheduler.runtime.runtime.expressionEvaluator.newInstance()
    try {
      val pass = eval.booleanValue(node.testExpr, contextItem.toList, receivedBindings.toMap, node.params)
      val ctx = if (contextItem.nonEmpty) {
        s" ${contextItem.head}(${contextItem.length})"
      } else {
        ""
      }
      tracer.trace(s"WHEN  ${node}: ${node.testExpr} = ${pass}$ctx", TraceEventManager.CHOOSE)
      pass
    } catch {
      case err: Throwable =>
        tracer.trace(s"WHEN  ${node}: ${node.testExpr} raised exception ${err}", TraceEventManager.CHOOSE)
        throw err
    }
  }

  override def run(): Unit = {
    super.run()
    startChildren()
    scheduler.finish(node)
  }

  override def reset(state: NodeState): Unit = {
    super.reset(state)
    contextItem.clear()
  }

  override def abort(): Unit = {
    super.abort()
    contextItem.clear()
  }
}
