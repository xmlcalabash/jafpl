package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{Node, WhenStart}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GException, GGuardResult}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class WhenActor(private val monitor: ActorRef,
                                 override protected val runtime: GraphRuntime,
                                 override protected val node: WhenStart)
  extends StartActor(monitor, runtime, node) {

  private var readyToCheck = false
  private var contextItem = ListBuffer.empty[Message]
  logEvent = TraceEvent.WHEN

  override protected def readyToRun: Boolean = {
    super.readyToRun && node.state != NodeState.CHECKGUARD
  }

  override protected def reset(): Unit = {
    trace("WHENRST", s"$node.condition", logEvent)
    readyToCheck = false
    contextItem.clear()
    openInputs.add("condition")
    super.reset()
  }

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", logEvent)
    if (port == "condition") {
      consume(port, item)
    } else {
      super.input(from, fromPort, port, item)
    }
  }

  override def consume(port: String, item: Message): Unit = {
    trace("CONSUME", s"$node $port (${contextItem.size})", logEvent)
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
    if (port == "condition") {
      openInputs -= "condition"
      checkIfReady()
    } else {
      super.close(port)
    }
  }

  protected[runtime] def checkGuard(): Unit = {
    trace("CHKGUARD", s"$node", logEvent)
    readyToCheck = true
    node.state = NodeState.CHECKGUARD
    checkIfReady()
  }

  private def checkIfReady(): Unit = {
    trace("CHKREADY", s"$node checkIfReady: ready:$readyToCheck inputs:${openInputs.isEmpty}", logEvent)
    if (readyToCheck && openInputs.isEmpty) {
      readyToCheck = false
      node.state = NodeState.STARTED
      try {
        val eval = runtime.runtime.expressionEvaluator.newInstance()
        val pass = eval.booleanValue(node.testExpr, contextItem.toList, bindings.toMap, node.params)
        monitor ! GGuardResult(node, pass)
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
