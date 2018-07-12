package com.jafpl.graph

import com.jafpl.exceptions.JafplException
import com.jafpl.util.ItemTester

private[jafpl] class LoopWhileStart(override val graph: Graph,
                                    override protected val end: ContainerEnd,
                                    override val userLabel: Option[String],
                                    val tester: ItemTester)
  extends LoopStart(graph, end, userLabel) {

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
