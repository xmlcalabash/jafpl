package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{Edge, Splitter}
import com.jafpl.messages.Message

import scala.collection.mutable.ListBuffer

private[runtime] class SplitterActor(private val monitor: ActorRef,
                                     override protected val runtime: GraphRuntime,
                                     override protected val node: Splitter)
  extends AtomicActor(monitor, runtime, node) {

  private val edges = ListBuffer.empty[Edge]
  logEvent = TraceEvent.SPLITTER

  override protected def initialize(): Unit = {
    for (port <- node.outputs) {
      edges += node.outputEdge(port)
    }
    super.initialize()
    trace("SEDGES", s"$node $edges", TraceEvent.NMESSAGES)
  }

  override protected def input(port: String, item: Message): Unit = {
    trace("SINPUT", s"$node $port ($edges)", TraceEvent.NMESSAGES)
    for (edge <- edges) {
      sendMessage(edge.fromPort, item)
    }
  }
}
