package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.Sink
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphMonitor.GFinished

private[runtime] class SinkActor(private val monitor: ActorRef,
                                 private val runtime: GraphRuntime,
                                 private val node: Sink)
  extends NodeActor(monitor, runtime, node)  {

  var hasBeenReset = false

  override protected def input(port: String, item: Message): Unit = {
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
