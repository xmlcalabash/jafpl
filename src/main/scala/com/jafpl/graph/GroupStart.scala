package com.jafpl.graph

import com.jafpl.steps.{Manifold, ManifoldSpecification}

private[jafpl] class GroupStart(override val graph: Graph,
                                override val end: ContainerEnd,
                                private val manspec: ManifoldSpecification,
                                override val userLabel: Option[String])
  extends ContainerStart(graph, end, userLabel) {

  manifold = manspec

  private[graph] override def inputsOk(): Boolean = {
    // Arbitrary inputs are ok on group
    true
  }

}
