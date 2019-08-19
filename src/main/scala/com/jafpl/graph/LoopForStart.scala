package com.jafpl.graph

import com.jafpl.messages.Message
import com.jafpl.steps.ManifoldSpecification

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[jafpl] class LoopForStart(override val graph: Graph,
                                  override protected val end: ContainerEnd,
                                  override val userLabel: Option[String],
                                  val countFrom: Long,
                                  val countTo: Long,
                                  val countBy: Long,
                                  private val manspec: ManifoldSpecification)
  extends LoopStart(graph, end, userLabel) {

  manifold = manspec

  override def inputsOk(): Boolean = {
    inputs.isEmpty
  }
}
