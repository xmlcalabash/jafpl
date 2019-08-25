package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.AtomicNode
import com.jafpl.messages.Message

private[runtime] class OutputActor(private val monitor: ActorRef,
                                   override protected val runtime: GraphRuntime,
                                   override protected val node: AtomicNode,
                                   private val consumer: OutputProxy)
  extends AtomicActor(monitor, runtime, node) {
  logEvent = TraceEvent.OUTPUT

  override protected def input(port: String, message: Message): Unit = {
    consumer.consume(port, message)
  }
}
