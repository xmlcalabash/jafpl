package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{Node, Sink}
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphMonitor.GFinished
import com.jafpl.steps.DataConsumer

private[runtime] class SinkActor(private val monitor: ActorRef,
                                 private val runtime: GraphRuntime,
                                 private val node: Sink)
  extends NodeActor(monitor, runtime, node) with DataConsumer {

  var hasBeenReset = false

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    receive(port, item)
  }

  override def id: String = node.id
  override def receive(port: String, item: Message): Unit = {
    // Oops, I dropped it on the floor
  }

  override protected def reset(): Unit = {
    readyToRun = true
    hasBeenReset = true
    openInputs.clear()
  }

  override protected def run(): Unit = {
    trace(s"RUNSINK↴ $node", "StepIO")
    monitor ! GFinished(node)
  }
}
