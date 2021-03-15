package com.jafpl.graph

import com.jafpl.graph.JoinMode.JoinMode
import com.jafpl.steps.{ManifoldSpecification, PortSpecification}

private[jafpl] class Joiner(override val graph: Graph, val mode: JoinMode) extends AtomicNode(graph, None, None) with ManifoldSpecification {
  override def inputsOk() = true

  override def outputsOk(): Boolean = {
    (outputs.size == 1) && outputs.contains("result")
  }

  def inputSpec: PortSpecification = PortSpecification.ANY
  def outputSpec: PortSpecification = PortSpecification.ANY
  override def manifold: Option[ManifoldSpecification] = Some(this)
}
