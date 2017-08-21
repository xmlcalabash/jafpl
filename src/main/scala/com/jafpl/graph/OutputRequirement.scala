package com.jafpl.graph

private[jafpl] class OutputRequirement(override val graph: Graph,
                                       override val label: String) extends Node(graph,None,Some(label)) {
  override def inputsOk() = true
  override def outputsOk() = true
}
