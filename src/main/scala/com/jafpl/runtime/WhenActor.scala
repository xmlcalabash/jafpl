package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.WhenStart
import com.jafpl.messages.Message
import com.jafpl.runtime.NodeActor.NGuardReport

import scala.collection.mutable.ListBuffer

private[runtime] class WhenActor(private val monitor: ActorRef,
                                  override protected val runtime: GraphRuntime,
                                  override protected val node: WhenStart) extends StartActor(monitor, runtime, node) {
  private var contextItem = ListBuffer.empty[Message]
  private var contextReady = false
  private var guardReady = false
  logEvent = TraceEvent.WHEN

  override protected def input(port: String, message: Message): Unit = {
    if (port == "condition") {
      contextItem += message
    } else {
      super.input(port, message)
    }
  }

  override protected def close(port: String): Unit = {
    super.close(port)
    if (port == "condition") {
      contextReady = true
      if (guardReady) {
        guardCheck()
      }
    }
  }

  override protected def reset(): Unit = {
    contextItem.clear()
    contextReady = false
    guardReady = false
    super.reset()
  }

  protected[runtime] def guardCheck(): Unit = {
    guardReady = true
    if (!contextReady) {
      return
    }

    val eval = runtime.runtime.expressionEvaluator.newInstance()
    val pass = eval.booleanValue(node.testExpr, contextItem.toList, bindings.toMap, node.params)
    parent ! NGuardReport(node, pass)
  }
}
