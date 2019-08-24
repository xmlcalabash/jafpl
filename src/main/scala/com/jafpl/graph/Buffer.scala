package com.jafpl.graph

private[jafpl] class Buffer(override val graph: Graph) extends AtomicNode(graph, None, None) {
  override def inputsOk(): Boolean = {
    (inputs.size == 1) && inputs.contains("source")
  }

  override def outputsOk(): Boolean = {
    (outputs.size == 1) && outputs.contains("result")
  }
}
