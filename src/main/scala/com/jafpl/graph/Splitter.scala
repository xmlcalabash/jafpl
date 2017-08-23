package com.jafpl.graph

private[jafpl] class Splitter(override val graph: Graph) extends Node(graph, None, None) {
  // Note: the inputs can be either a single port or a binding.
  override def inputsOk() = true
  override def outputsOk() = true
}
