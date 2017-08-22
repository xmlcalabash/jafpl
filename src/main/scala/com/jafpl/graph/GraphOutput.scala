package com.jafpl.graph

private[jafpl] class GraphOutput(override val graph: Graph,
                                 val name: String) extends Node(graph,None,Some(name)) {
  override def inputsOk() = true
  override def outputsOk() = true
}
