package com.jafpl.graph

import com.jafpl.graph.JoinMode.JoinMode

private[jafpl] class Edge(val graph: Graph,
                          val from: Node, val fromPort: String,
                          val to: Node, val toPort: String,
                          val mode: JoinMode) {
  def this(graph: Graph, from: Node, fromPort: String, to: Node, toPort: String) = {
    this(graph,from,fromPort,to,toPort,JoinMode.MIXED)
  }

  override def toString: String = {
    s"${from}.${fromPort} -> ${to}.${toPort} (${mode})"
  }
}
