package com.jafpl.graph

private[jafpl] class Sink(override val graph: Graph) extends AtomicNode(graph, None, None) {
  // Note: the inputs can be either a single port or a binding.
  override def inputsOk() = true
  override def outputsOk() = true
}
