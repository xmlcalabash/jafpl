package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.Node
import com.jafpl.messages.{Metadata, PipelineMessage}
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput}

private[runtime] class InputActor(private val monitor: ActorRef,
                                  override protected val runtime: GraphRuntime,
                                  override protected val node: Node,
                                  private val consumer: InputProxy)
  extends NodeActor(monitor, runtime, node, consumer) {
  logEvent = TraceEvent.INPUT

  override protected def start(): Unit = {
    trace("START", s"$node", logEvent)
    readyToRun = true
    runIfReady()
  }

  private def runIfReady(): Unit = {
    trace("RUNIFREADY", s"$node ready:$readyToRun closed:${consumer.closed}", logEvent)
    if (readyToRun && consumer.closed) {
      for (item <- consumer.items) {
        monitor ! GOutput(node, "result", item)
      }
      consumer.clear()
      monitor ! GClose(node, "result")
      monitor ! GFinished(node)
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Input]"
  }
}
