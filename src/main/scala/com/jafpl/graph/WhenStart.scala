package com.jafpl.graph

import com.jafpl.steps.ManifoldSpecification

private[jafpl] class WhenStart(override val graph: Graph,
                               override protected val end: ContainerEnd,
                               override val userLabel: Option[String],
                               private val manspec: ManifoldSpecification,
                               val testExpr: Any)
  extends ContainerStart(graph, end, userLabel) {

  manifold = manspec

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
