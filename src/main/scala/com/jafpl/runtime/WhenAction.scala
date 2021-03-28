package com.jafpl.runtime

import com.jafpl.graph.WhenStart
import com.jafpl.messages.Message
import com.jafpl.runtime.NodeState.NodeState
import com.jafpl.util.TraceEventManager

import scala.collection.mutable.ListBuffer

class WhenAction(override val node: WhenStart) extends ContainerAction(node) {
  def test: Boolean = {
    val eval = scheduler.runtime.runtime.expressionEvaluator.newInstance()
    try {
      eval.setContextItem(List())
      val contextItem = received("condition")
      val usecollection = eval.booleanValue(node.collectionExpr, receivedBindings.toMap, node.params)
      if (usecollection) {
        eval.setContextCollection(contextItem)
      } else {
        eval.setContextItem(contextItem)
      }

      val pass = eval.booleanValue(node.testExpr, receivedBindings.toMap, node.params)

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
}
