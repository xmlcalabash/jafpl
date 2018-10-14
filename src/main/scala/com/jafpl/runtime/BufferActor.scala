package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{Buffer, Node}
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput}
import com.jafpl.steps.DataConsumer

import scala.collection.mutable.ListBuffer

private[runtime] class BufferActor(private val monitor: ActorRef,
                                   override protected val runtime: GraphRuntime,
                                   override protected val node: Buffer)
  extends NodeActor(monitor, runtime, node) with DataConsumer {

  private var hasBeenReset = false
  private var buffer = ListBuffer.empty[Message]

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", TraceEvent.METHODS)
    receive(port, item)
  }

  override def id: String = node.id
  override def receive(port: String, item: Message): Unit = {
    trace("RECEIVE", s"$node $port", TraceEvent.METHODS)
    buffer += item
    monitor ! GOutput(node, "result", item)
  }

  override protected def reset(): Unit = {
    trace("RESET", s"$node", TraceEvent.METHODS)
    readyToRun = true
    hasBeenReset = true
    openInputs.clear()
  }

  override protected def run(): Unit = {
    trace("RUN", s"$node", TraceEvent.METHODS)
    if (hasBeenReset) {
      for (item <- buffer) {
        monitor ! GOutput(node, "result", item)
      }
    }
    for (output <- node.outputs) {
      monitor ! GClose(node, output)
    }
    monitor ! GFinished(node)
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Buffer]"
  }
}
