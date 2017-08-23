package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.GraphException
import com.jafpl.graph.WhenStart
import com.jafpl.messages.BindingMessage
import com.jafpl.runtime.GraphMonitor.{GGuardResult, GStart}

import scala.collection.mutable

private[runtime] class WhenActor(private val monitor: ActorRef,
                                 private val runtime: GraphRuntime,
                                 private val node: WhenStart) extends StartActor(monitor, runtime, node)  {
  var readyToCheck = false
  var recvContext = false
  var contextItem = Option.empty[Any]
  val bindings = mutable.HashMap.empty[String, Any]

  override protected def input(port: String, item: Any): Unit = {
    if (port == "condition") {
      contextItem = Some(item)
    } else if (port == "#bindings") {
      item match {
        case msg: BindingMessage =>
          bindings.put(msg.name, msg.item)
        case _ => throw new GraphException(s"Unexpected message on $port", node.location)
      }
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
