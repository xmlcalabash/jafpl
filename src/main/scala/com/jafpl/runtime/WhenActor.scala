package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.{Node, WhenStart}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GException, GGuardResult, GStart}
import com.jafpl.steps.DataConsumer

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class WhenActor(private val monitor: ActorRef,
                                 private val runtime: GraphRuntime,
                                 private val node: WhenStart)
  extends StartActor(monitor, runtime, node) with DataConsumer {

  private var readyToCheck = false
  private var contextItem = ListBuffer.empty[Message]
  private val bindings = mutable.HashMap.empty[String, Message]

  override protected def input(from: Node, fromPort: String, port: String, msg: Message): Unit = {
    receive(port, msg)
  }

  override def id: String = node.id
  override def receive(port: String, item: Message): Unit = {
    item match {
      case item: ItemMessage =>
        assert(port == "condition")
        contextItem += item
      case binding: BindingMessage =>
        assert(port == "#bindings")
        trace(s"$node received binding for ${binding.name}", "Bindings")
        bindings.put(binding.name, binding.message)
      case _ =>
        monitor ! GException(None,
          new PipelineException("badmessage", s"Unexpected message on $port", node.location))
        return
    }
  }

  override protected def close(port: String): Unit = {
    trace(s"$node closed $port", "StepIO")
    super.close(port)
    checkIfReady()
  }

  override protected def start(): Unit = {
    commonStart()
    for (child <- node.children) {
      monitor ! GStart(child)
    }
  }

  protected[runtime] def checkGuard(): Unit = {
    trace(s"$node checkGuard", "StepExec")
    readyToCheck = true
    checkIfReady()
  }

  private def checkIfReady(): Unit = {
    trace(s"$node checkIfReady: ready:$readyToCheck inputs:${openInputs.isEmpty}", "StepExec")
    if (readyToCheck && openInputs.isEmpty) {
      val eval = runtime.runtime.expressionEvaluator.newInstance()
      val pass = eval.booleanValue(node.testExpr, contextItem.toList, bindings.toMap, None)
      monitor ! GGuardResult(node, pass)
    }
  }
}
