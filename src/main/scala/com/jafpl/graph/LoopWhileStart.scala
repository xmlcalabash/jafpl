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

  private var _done = false
  manifold = manspec

  protected[jafpl] def done: Boolean = _done
  protected[jafpl] def done_=(state: Boolean): Unit = {
    _done = state
  }

  override def inputsOk(): Boolean = {
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
      graph.error(JafplException.whileLoopTestRequired(location))
    }

    hasTest
  }
}
