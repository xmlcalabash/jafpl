package com.jafpl.graph

import com.jafpl.exceptions.GraphException
import com.jafpl.util.ItemTester

private[jafpl] class LoopForStart(override val graph: Graph,
                                  override protected val end: ContainerEnd,
                                  override val userLabel: Option[String],
                                  val countFrom: Long,
                                  val countTo: Long,
                                  val countBy: Long)
  extends LoopStart(graph, end, userLabel) {

  override def inputsOk(): Boolean = {
    inputs.isEmpty
  }
}
