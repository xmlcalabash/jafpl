package com.jafpl.graph

import com.jafpl.steps.{Manifold, ManifoldSpecification, PortCardinality, PortSpecification}

private[jafpl] class EmptySource(override val graph: Graph) extends AtomicNode(graph, None, None) {
  // An empty source has no inputs and can produce no outputs.
  private val _manifold = new Manifold(new PortSpecification(Map.empty[String, PortCardinality]), Manifold.singlePort("result", 0, 0))

  override def inputsOk() = true
  override def outputsOk() = true

  override def manifold: Option[ManifoldSpecification] = {
    Some(_manifold)
  }
}
