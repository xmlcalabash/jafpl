package com.jafpl.graph

private[jafpl] class Splitter(override val graph: Graph) extends Node(graph, None, None) {
  override def inputsOk(): Boolean = {
    (inputs.size == 1) && inputs.contains("source")
  }

  override def outputsOk() = true
}
