package com.jafpl.graph

/** A looping container.
  *
  * Loops need buffers when they read from steps outside the loop.
  *
  * In practice, containers are represented by a start and an end.
  *
  * @constructor A container in the pipeline graph.
  * @param graph The graph into which this node is to be inserted.
  * @param end The end of this container.
  * @param userLabel An optional user-defined label.
  */
abstract class LoopStart(override val graph: Graph,
                         override protected val end: ContainerEnd,
                         override val userLabel: Option[String]) extends ContainerStart(graph, end, userLabel) {
}
