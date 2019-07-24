package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{Node, WhenStart}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GException, GGuardResult, GStart}
import com.jafpl.steps.DataConsumer

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class WhenActor(private val monitor: ActorRef,
                                 override protected val runtime: GraphRuntime,
                                 override protected val node: WhenStart)
  extends StartActor(monitor, runtime, node) with DataConsumer {

  private var readyToCheck = false
  private var abortCheck = false
  private var contextItem = ListBuffer.empty[Message]
  private val bindings = mutable.HashMap.empty[String, Message]
  logEvent = TraceEvent.WHEN

  override protected def input(from: Node, fromPort: String, port: String, msg: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", logEvent)
    receive(port, msg)
  }

  override def receive(port: String, item: Message): Unit = {
    trace("RECEIVE", s"$node $port", logEvent)
    item match {
      case item: ItemMessage =>
        assert(port == "condition")
        contextItem += item
      case binding: BindingMessage =>
        assert(port == "#bindings")
        trace("WHENBIND", s"$node received binding for ${binding.name}", TraceEvent.BINDINGS)
        bindings.put(binding.name, binding.message)
      case _ =>
        monitor ! GException(None,
          JafplException.unexpectedMessage(item.toString, port, node.location))
    }
  }

  override protected def close(port: String): Unit = {
    trace("CLOSE", s"$node $port", logEvent)
    super.close(port)
    checkIfReady()
  }

  override protected def start(): Unit = {
    trace("START", s"$node", logEvent)
    commonStart()
    for (child <- node.children) {
      monitor ! GStart(child)
    }
  }

  protected[runtime] def checkGuard(): Unit = {
    trace("CHKGUARD", s"$node", logEvent)
    readyToCheck = true
    checkIfReady()
  }

  protected[runtime] def abortGuard(): Unit = {
    trace("ABTGUARD", s"$node", logEvent)
    abortCheck = true
    readyToCheck = true
    checkIfReady()
  }

  private def checkIfReady(): Unit = {
    trace("CHKREADY", s"$node checkIfReady: ready:$readyToCheck inputs:${openInputs.isEmpty}", logEvent)
    if (readyToCheck && openInputs.isEmpty) {
      try {
        if (abortCheck) {
          monitor ! GGuardResult(node, false)
        } else {
          val eval = runtime.runtime.expressionEvaluator.newInstance()
          val pass = eval.booleanValue(node.testExpr, contextItem.toList, bindings.toMap, node.params)
          monitor ! GGuardResult(node, pass)
        }
      } catch {
        case ex: Exception =>
          monitor ! GException(Some(node), ex)
      }
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [When]"
  }
}
