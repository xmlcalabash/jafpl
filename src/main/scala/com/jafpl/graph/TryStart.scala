package com.jafpl.graph

private[jafpl] class TryStart(override val graph: Graph,
                              override protected val end: ContainerEnd,
                              override val userLabel: Option[String]) extends ContainerStart(graph, end, userLabel) {
}
