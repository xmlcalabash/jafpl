package com.jafpl.graph

import com.jafpl.steps.{ManifoldSpecification, PortSpecification}

private[jafpl] class Buffer(override val graph: Graph) extends AtomicNode(graph, None, None) with ManifoldSpecification {
  override def inputsOk(): Boolean = {
    (inputs.size == 1) && inputs.contains("source")
  }

  override def outputsOk(): Boolean = {
    (outputs.size == 1) && outputs.contains("result")
  }

  def inputSpec: PortSpecification = PortSpecification.ANY
  def outputSpec: PortSpecification = PortSpecification.ANY
  override def manifold: Option[ManifoldSpecification] = Some(this)
}
