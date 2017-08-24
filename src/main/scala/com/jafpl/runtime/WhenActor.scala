package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.WhenStart
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GException, GGuardResult, GStart}

import scala.collection.mutable

private[runtime] class WhenActor(private val monitor: ActorRef,
                                 private val runtime: GraphRuntime,
                                 private val node: WhenStart) extends StartActor(monitor, runtime, node)  {
  var readyToCheck = false
  var recvContext = false
  var contextItem = Option.empty[ItemMessage]
  val bindings = mutable.HashMap.empty[String, Any]

  override protected def input(port: String, msg: Message): Unit = {
    msg match {
      case item: ItemMessage =>
        assert(port == "condition")
        contextItem = Some(item)
      case binding: BindingMessage =>
        assert(port == "#bindings")
        bindings.put(binding.name, binding.item)
      case _ =>
        monitor ! GException(None,
          new PipelineException("badmessage", s"Unexpected message on $port", node.location))
        return
    }
  }

  override protected def close(port: String): Unit = {
    super.close(port)
    if (port == "condition") {
      recvContext = true
    }
    checkIfReady()
  }

  override protected def start(): Unit = {
    readyToRun = true
    for (child <- node.children) {
      monitor ! GStart(child)
    }
  }

  override protected def checkGuard(): Unit = {
    readyToCheck = true
    checkIfReady()
  }

  private def checkIfReady(): Unit = {
    if (readyToCheck && recvContext) {
      val pass = runtime.dynamicContext.expressionEvaluator().booleanValue(node.testExpr, contextItem, Some(bindings.toMap))
      monitor ! GGuardResult(node, pass)
    }
  }
}
