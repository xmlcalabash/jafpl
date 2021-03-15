package com.jafpl.graph

import com.jafpl.steps.{ManifoldSpecification, PortSpecification}

private[jafpl] class Sink(override val graph: Graph) extends AtomicNode(graph, None, None) with ManifoldSpecification {
  // Note: the inputs can be either a single port or a binding.
  override def inputsOk() = true
  override def outputsOk() = true

  def inputSpec: PortSpecification = PortSpecification.ANY
  def outputSpec: PortSpecification = PortSpecification.NONE
  override def manifold: Option[ManifoldSpecification] = Some(this)
}
