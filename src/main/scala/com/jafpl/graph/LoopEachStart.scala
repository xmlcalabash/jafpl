package com.jafpl.graph

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
                                   override val userLabel: Option[String])
  extends LoopStart(graph, end, userLabel) {

  override def inputsOk(): Boolean = {
    var hasSource = false

    if (inputs.nonEmpty) {
      var valid = true
      for (port <- inputs) {
        if (port != "#bindings" && port != "source") {
          println("Invalid binding on " + this + ": " + port)
          valid = false
        }
        hasSource = hasSource || (port == "source")
      }
      valid && hasSource
    } else {
      true
    }
  }

}
