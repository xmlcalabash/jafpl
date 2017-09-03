package com.jafpl.graph

/** The catch block of a try catch.
  *
  * Catch containers are created with the `addCatch` method of [[com.jafpl.graph.TryCatchStart]].
  *
  * @param graph The graph into which this node is to be inserted.
  * @param end The end of this container.
  * @param userLabel An optional user-defined label.
  */
class FinallyStart private[jafpl](override val graph: Graph,
                                  override protected val end: ContainerEnd,
                                  override val userLabel: Option[String])
  extends ContainerStart(graph, end, userLabel) {

}
