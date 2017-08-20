package com.jafpl.graph

private[jafpl] class WhenStart(override val graph: Graph,
                               override val end: ContainerEnd,
                               override val userLabel: Option[String],
                               val testExpr: String)
  extends ContainerStart(graph, end, userLabel) {

  override def inputsOk(): Boolean = {
    if (inputs.nonEmpty) {
      var valid = true
      for (port <- inputs) {
        if (port != "#bindings" && port != "condition") {
          println("Invalid binding on " + this + ": " + port)
          valid = false
        }
      }
      valid
    } else {
      true
    }
  }

}
