package com.jafpl.graph

import com.jafpl.steps.Step

private[graph] class AtomicNode(override val graph: Graph,
                 override val step: Option[Step],
                 override val userLabel: Option[String]) extends Node(graph,step,userLabel) {
  override def inputsOk() = true
  override def outputsOk() = true
}
