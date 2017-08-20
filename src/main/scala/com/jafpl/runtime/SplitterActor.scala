package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{Edge, Splitter}
import com.jafpl.runtime.GraphMonitor.GOutput

import scala.collection.mutable.ListBuffer

private[runtime] class SplitterActor(private val monitor: ActorRef,
                                     private val runtime: GraphRuntime,
                                     private val node: Splitter)
  extends NodeActor(monitor, runtime, node)  {

  var edges: Option[ListBuffer[Edge]] = None

  override protected def input(port: String, item: Any): Unit = {
    if (edges.isEmpty) {
      val outbound = ListBuffer.empty[Edge]
      for (port <- node.outputs) {
        outbound += node.outputEdge(port)
      }
      edges = Some(outbound)
    }

    if (edges.isDefined) {
      for (edge <- edges.get) {
        monitor ! GOutput(node, edge.fromPort, item)
      }
    }
  }
}
