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

  override protected def start(): Unit = {
    for (port <- node.outputs) {
      edges += node.outputEdge(port)
    }
    super.start()
  }

  override protected def input(port: String, item: Message): Unit = {
    for (edge <- edges) {
      sendMessage(edge.fromPort, item)
    }
  }
}
