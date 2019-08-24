package com.jafpl.graph

import com.jafpl.graph.JoinMode.JoinMode

private[jafpl] class Joiner(override val graph: Graph, val mode: JoinMode) extends AtomicNode(graph, None, None) {

  override def inputsOk() = true

  override def outputsOk(): Boolean = {
    (outputs.size == 1) && outputs.contains("result")
  }
}
