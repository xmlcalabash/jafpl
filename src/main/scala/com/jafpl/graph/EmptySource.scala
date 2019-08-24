package com.jafpl.graph

private[jafpl] class EmptySource(override val graph: Graph) extends AtomicNode(graph, None, None) {
  override def inputsOk() = true
  override def outputsOk() = true
}
