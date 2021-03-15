package com.jafpl.graph

import com.jafpl.steps.{Manifold, ManifoldSpecification}

private[jafpl] class TryStart(override val graph: Graph,
                              override protected val end: ContainerEnd,
                              override val userLabel: Option[String]) extends ContainerStart(graph, end, userLabel) {
  // FIXME: Is this right?
  override def manifold: Option[ManifoldSpecification] = Some(Manifold.ALLOW_ANY)
}
