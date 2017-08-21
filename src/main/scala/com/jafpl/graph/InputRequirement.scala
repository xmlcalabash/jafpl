package com.jafpl.graph

import com.jafpl.steps.Step

private[jafpl] class InputRequirement(override val graph: Graph,
                                      override val label: String) extends Node(graph,None,Some(label)) {
  override def inputsOk() = true
  override def outputsOk() = true
}
