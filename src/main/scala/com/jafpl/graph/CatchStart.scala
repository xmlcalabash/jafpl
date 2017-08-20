package com.jafpl.graph

private[jafpl] class CatchStart(override val graph: Graph,
                                override val end: ContainerEnd,
                                override val userLabel: Option[String],
                                val codes: List[String])
  extends ContainerStart(graph, end, userLabel) {
}
