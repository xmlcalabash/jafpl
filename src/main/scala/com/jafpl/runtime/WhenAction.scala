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
      val contextItem = received("condition")
      val pass = eval.booleanValue(node.testExpr, contextItem, receivedBindings.toMap, node.params)
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

    logger.info(s"Running when ${node.userLabel.getOrElse("")}")

    startChildren()
    scheduler.finish(node)
  }
}
