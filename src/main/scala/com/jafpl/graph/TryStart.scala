package com.jafpl.graph

import com.jafpl.steps.{Manifold, ManifoldSpecification}

private[jafpl] class TryStart(override val graph: Graph,
                              override protected val end: ContainerEnd,
                              override val userLabel: Option[String],
                              private val manspec: ManifoldSpecification) extends ContainerStart(graph, end, userLabel) {
  manifold = manspec
}
