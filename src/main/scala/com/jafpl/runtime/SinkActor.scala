package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.Sink
import com.jafpl.messages.Message

private[runtime] class SinkActor(private val monitor: ActorRef,
                                 override protected val runtime: GraphRuntime,
                                 override protected val node: Sink) extends AtomicActor(monitor, runtime, node) {
  logEvent = TraceEvent.SINK

  override protected def input(port: String, item: Message): Unit = {
    // Oops, I dropped it on the floor
  }
}
