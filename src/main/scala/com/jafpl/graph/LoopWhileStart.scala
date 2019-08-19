package com.jafpl.graph

import com.jafpl.exceptions.JafplException
import com.jafpl.messages.Message
import com.jafpl.steps.ManifoldSpecification
import com.jafpl.util.ItemTester

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[jafpl] class LoopWhileStart(override val graph: Graph,
                                    override protected val end: ContainerEnd,
                                    override val userLabel: Option[String],
                                    private val manspec: ManifoldSpecification,
                                    val tester: ItemTester,
                                    val returnAll: Boolean)
  extends LoopStart(graph, end, userLabel) {

  manifold = manspec

  override def inputsOk(): Boolean = {
    var hasSource = false

    if (inputs.nonEmpty) {
      var valid = true
      for (port <- inputs) {
        if (port != "#bindings" && port != "source") {
          graph.error(JafplException.invalidInputPort(port, this.toString, location))
          valid = false
        }
        hasSource = hasSource || (port == "source")
      }
      valid && hasSource
    } else {
      true
    }
  }

  override def outputsOk(): Boolean = {
    var hasTest = false

    for (port <- end.inputs) {
      hasTest = hasTest || (port == "test")
    }

    if (!hasTest) {
      graph.error(JafplException.whileLoopTestRequired(location))
    }

    hasTest
  }
}
