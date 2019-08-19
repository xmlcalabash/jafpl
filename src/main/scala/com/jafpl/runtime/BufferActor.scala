package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{Buffer, Node}
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput, GResetFinished}

import scala.collection.mutable.ListBuffer

private[runtime] class BufferActor(private val monitor: ActorRef,
                                   override protected val runtime: GraphRuntime,
                                   override protected val node: Buffer)
  extends NodeActor(monitor, runtime, node) {
  private var hasBeenReset = false
  private var buffer = ListBuffer.empty[Message]
  logEvent = TraceEvent.BUFFER

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", logEvent)
    buffer += item
    monitor ! GOutput(node, "result", item)
  }

  override protected def reset(): Unit = {
    trace("RESET", s"$node", logEvent)
    started = true
    hasBeenReset = true
    openInputs.clear()
    monitor ! GResetFinished(node)
  }

  override protected def run(): Unit = {
    trace("RUN", s"$node", logEvent)
    if (hasBeenReset) {
      for (item <- buffer) {
        monitor ! GOutput(node, "result", item)
      }
    }
    for (output <- node.outputs) {
      monitor ! GClose(node, output)
    }
    node.state = NodeState.FINISHED
    monitor ! GFinished(node)
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Buffer]"
  }
}
