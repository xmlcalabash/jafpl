package com.jafpl.graph

private[jafpl] class GraphInput(override val graph: Graph,
                                val name: String) extends Node(graph,None,Some(name)) {
  override def inputsOk() = true
  override def outputsOk() = true
}
