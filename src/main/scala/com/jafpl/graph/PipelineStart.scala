package com.jafpl.graph

private[jafpl] class PipelineStart(override val graph: Graph,
                                   override val end: ContainerEnd,
                                   override val userLabel: Option[String])
  extends ContainerStart(graph, end, userLabel) {

  override def inputsOk() = true

  override def outputsOk() = true

}
