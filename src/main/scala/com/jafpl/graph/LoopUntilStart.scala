package com.jafpl.graph

import com.jafpl.exceptions.JafplException
import com.jafpl.steps.ManifoldSpecification
import com.jafpl.util.ItemComparator

private[jafpl] class LoopUntilStart(override val graph: Graph,
                                    override protected val end: ContainerEnd,
                                    override val userLabel: Option[String],
                                    private val manspec: ManifoldSpecification,
                                    val returnAll: Boolean,
                                    val comparator: ItemComparator)
  extends LoopStart(graph, end, userLabel) {

  manifold = manspec

  override def inputsOk(): Boolean = {
    var hasSource = false

    if (inputs.nonEmpty) {
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
      graph.error(JafplException.untilLoopTestRequired(location))
    }

    hasTest
  }
}
