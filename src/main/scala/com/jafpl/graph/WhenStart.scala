package com.jafpl.graph

import com.jafpl.steps.ManifoldSpecification

private[jafpl] class WhenStart(override val graph: Graph,
                               override protected val end: ContainerEnd,
                               override val userLabel: Option[String],
                               private val manspec: ManifoldSpecification,
                               val testExpr: Any,
                               val params: Option[BindingParams])
  extends ContainerStart(graph, end, userLabel) {

  def this(graph: Graph, end: ContainerEnd, userLabel: Option[String], manspec: ManifoldSpecification, testExpr: Any) {
    this(graph, end, userLabel, manspec, testExpr, None)
  }

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
