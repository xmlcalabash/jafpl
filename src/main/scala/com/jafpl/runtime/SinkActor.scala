package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{Node, Sink}
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphMonitor.GFinished
import com.jafpl.steps.DataConsumer

private[runtime] class SinkActor(private val monitor: ActorRef,
                                 override protected val runtime: GraphRuntime,
                                 override protected val node: Sink)
  extends NodeActor(monitor, runtime, node) with DataConsumer {

  var hasBeenReset = false

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", TraceEvent.METHODS)
    receive(port, item)
  }

  override def receive(port: String, item: Message): Unit = {
    trace("RECEIVE", s"$node $port (to /dev/null)", TraceEvent.METHODS)
    // Oops, I dropped it on the floor
  }

  override protected def reset(): Unit = {
    trace("RESET", s"$node", TraceEvent.METHODS)
    readyToRun = true
    hasBeenReset = true
    openInputs.clear()
  }

  override protected def run(): Unit = {
    trace("RUN", s"$node", TraceEvent.METHODS)
    monitor ! GFinished(node)
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Sink]"
  }
}
