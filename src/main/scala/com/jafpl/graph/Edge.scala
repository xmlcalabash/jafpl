package com.jafpl.graph

private[jafpl] class Edge(val graph: Graph,
                          val from: Node, val fromPort: String,
                          val to: Node, val toPort: String,
                          val ordered: Boolean) {
  def this(graph: Graph, from: Node, fromPort: String, to: Node, toPort: String) = {
    this(graph,from,fromPort,to,toPort,ordered=false)
  }

  override def toString: String = {
    from + "." + fromPort + " -> " + to + "." + toPort + " (" + ordered + ")"
  }
}
