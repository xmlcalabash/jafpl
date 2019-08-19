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
  logEvent = TraceEvent.OUTPUT

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node.$port from $from.$fromPort", logEvent)

    item match {
      case item: ItemMessage =>
        if (consumer.provider.isDefined) {
          trace("DELIVER→", s"$node.$port → ${consumer.provider.get}.$port", TraceEvent.STEPIO)
          consumer.provider.get.consume(port, item)
        } else {
          trace("DELIVER↴", s"$node.$port (no consumer)", logEvent)
        }
      case _ =>
        monitor ! GException(None,
          JafplException.unexpectedMessage(item.toString, port, node.location))
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Output]"
  }
}
