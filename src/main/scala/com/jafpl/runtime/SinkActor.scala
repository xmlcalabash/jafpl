package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{Node, Sink}
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphMonitor.GFinished
import com.jafpl.steps.DataConsumer

private[runtime] class SinkActor(private val monitor: ActorRef,
                                 override protected val runtime: GraphRuntime,
                                 override protected val node: Sink)
  extends NodeActor(monitor, runtime, node) {
  logEvent = TraceEvent.SINK

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", logEvent)
    // Oops, I dropped it on the floor
  }

  override protected def run(): Unit = {
    trace("RUN", s"$node", logEvent)
    node.state = NodeState.FINISHED
    monitor ! GFinished(node)
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Sink]"
  }
}
