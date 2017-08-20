package com.jafpl.graph

private[jafpl] class Edge(val graph: Graph,
                          val from: Node, val fromPort: String,
                          val to: Node, val toPort: String) {
  override def toString: String = {
    from + "." + fromPort + " -> " + to + "." + toPort
  }
}
