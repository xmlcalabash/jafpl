package com.jafpl.graph

import com.jafpl.graph.JoinMode.JoinMode

private[jafpl] class Joiner(override val graph: Graph, val mode: JoinMode) extends Node(graph, None, None) {

  override def inputsOk() = true

  override def outputsOk(): Boolean = {
    if (mode == JoinMode.PRIORITY) {
      (inputs.size == 2) && (outputs.size == 1) && outputs.contains("result")
    } else {
      (outputs.size == 1) && outputs.contains("result")
    }
  }
}
