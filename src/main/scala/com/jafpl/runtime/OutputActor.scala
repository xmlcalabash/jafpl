package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.Node
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.GException

private[runtime] class OutputActor(private val monitor: ActorRef,
                                   override protected val runtime: GraphRuntime,
                                   override protected val node: Node,
                                   private val consumer: OutputProxy)
  extends NodeActor(monitor, runtime, node, consumer) {
  private var closed = false

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", TraceEvent.METHODS)

    item match {
      case item: ItemMessage =>
        if (consumer.provider.isDefined) {
          trace("DELIVER→", s"$node.$port → ${consumer.provider.get}.$port", TraceEvent.STEPIO)
          consumer.provider.get.receive(port, item)
        } else {
          trace("DELIVER↴", s"$node.$port (no consumer)", TraceEvent.METHODS)
        }
      case _ =>
        monitor ! GException(None,
          JafplException.unexpectedMessage(item.toString, port, node.location))
    }
  }

  override protected def close(port: String): Unit = {
    trace("CLOSE", s"$node", TraceEvent.METHODS)
    closed = true
    runIfReady()
  }

  override protected def start(): Unit = {
    trace("START", s"$node", TraceEvent.METHODS)
    readyToRun = true
    runIfReady()
  }

  private def runIfReady(): Unit = {
    trace("RUNIFREADY", s"$node ready:$readyToRun closed:$closed", TraceEvent.METHODS)
    if (readyToRun && closed) {
      run()
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Output]"
  }
}
