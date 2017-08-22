package com.jafpl.graph

import com.jafpl.steps.Step

private[jafpl] class GraphInput(override val graph: Graph,
                                val name: String) extends Node(graph,None,Some(name)) {
  override def inputsOk() = true
  override def outputsOk() = true
}
