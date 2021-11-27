package com.jafpl.graph

import com.jafpl.steps.ManifoldSpecification

/** A for-each container.
  *
  * ForEach containers are created with the `addForEach` method of [[com.jafpl.graph.ContainerStart]].
  *
  * @param graph The graph into which this node is to be inserted.
  * @param end The end of this container.
  * @param userLabel An optional user-defined label.
  */
class LoopEachStart private[jafpl](override val graph: Graph,
                                   override protected val end: ContainerEnd,
                                   override val userLabel: Option[String],
                                   private val manspec: ManifoldSpecification)
  extends LoopStart(graph, end, userLabel) {

  manifold = manspec

  override def inputsOk(): Boolean = {
    val valid = true
    var count = 0
    for (port <- inputs) {
      if (port.startsWith("#depends") || port == "#bindings") {
        // these don't count
      } else {
        count += 1
      }
    }
    valid && (count == 1)
  }

}
