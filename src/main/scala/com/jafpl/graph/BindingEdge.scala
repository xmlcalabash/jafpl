package com.jafpl.graph

private[jafpl] class BindingEdge(override val graph: Graph,
                                 override val from: Binding,
                                 override val to: Node) extends Edge(graph, from, "result", to, "#bindings") {
  override def toString: String = {
    from + "." + from.name + " => " + to
  }
}
