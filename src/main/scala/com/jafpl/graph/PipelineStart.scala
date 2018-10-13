package com.jafpl.graph

import com.jafpl.steps.ManifoldSpecification

private[jafpl] class PipelineStart(override val graph: Graph,
                                   override protected val end: ContainerEnd,
                                   private val manspec: ManifoldSpecification,
                                   override val userLabel: Option[String])
  extends ContainerStart(graph, end, userLabel) {

  manifold = manspec

  override def inputsOk() = true

  override def outputsOk() = true

}
