package com.jafpl.graph

private[jafpl] class GroupStart(override val graph: Graph,
                                override val end: ContainerEnd,
                                override val userLabel: Option[String])
  extends ContainerStart(graph, end, userLabel) {

  private[graph] override def inputsOk(): Boolean = {
    // Arbitrary inputs are ok on group
    true
  }

}
