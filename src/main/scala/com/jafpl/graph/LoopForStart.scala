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

  // This buffer isn't really related to the node, but it needs to
  // be shared between the start and end actors, so this is a convenient
  // place to put it.
  protected[jafpl] val buffer = mutable.HashMap.empty[String, ListBuffer[Message]]

  manifold = manspec

  override def inputsOk(): Boolean = {
    inputs.isEmpty
  }
}
