package com.jafpl.graph

private[jafpl] class BindingEdge(override val graph: Graph,
                                 override val from: Binding,
                                 override val fromPort: String,
                                 override val to: Node) extends Edge(graph, from, fromPort, to, "#bindings") {

  def this(graph: Graph, from: Binding, to: Node) = {
    this(graph, from, "result", to)
  }
}
