package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.EmptySource
import com.jafpl.messages.Message

private[runtime] class EmptySourceActor(private val monitor: ActorRef,
                                        override protected val runtime: GraphRuntime,
                                        override protected val node: EmptySource) extends AtomicActor(monitor, runtime, node)  {
  logEvent = TraceEvent.EMPTY

  override protected def input(port: String, item: Message): Unit = {
    throw new RuntimeException("What do you mean input on an EmptySource?")
  }
}
