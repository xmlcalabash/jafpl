package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.AtomicNode

private[runtime] class InputActor(private val monitor: ActorRef,
                                  override protected val runtime: GraphRuntime,
                                  override protected val node: AtomicNode,
                                  private val consumer: InputProxy) extends AtomicActor(monitor, runtime, node) {
  logEvent = TraceEvent.INPUT

  override protected def run(): Unit = {
    for (message <- consumer.items) {
      sendMessage("result", message)
    }
    super.run()
  }
}
