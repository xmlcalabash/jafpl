package com.jafpl.graph

import com.jafpl.graph.GraphMonitor.GSubgraph
import com.jafpl.runtime.{CompoundStep, DefaultCompoundStart}

/**
  * Created by ndw on 10/2/16.
  */
class TryStart(graph: Graph, step: Option[CompoundStep], group: Node, nodes: List[Node]) extends DefaultCompoundStart(graph, step, nodes) {
  label = Some("_try_start")

  override private[graph] def run(): Unit = {
    // nop
  }

  override private[jafpl] def identifySubgraphs(): Unit = {
    val subgraph = List(group) ++ nodes
    graph.monitor ! GSubgraph(this, subgraph)
  }

  override def caught(exception: Throwable): Boolean = {
    for (node <- nodes) {
      node match {
        case start: CatchStart =>
          if (start.caught(exception)) {
            return true
          }
        case _ =>
          throw new GraphException("Invalid child of try: " + node.toString)
      }
    }
    false
  }
}
