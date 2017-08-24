package com.jafpl.graph

import com.jafpl.steps.Step

/** An atomic node.
  *
  * Atomic nodes are the most common pipeline extension point. The implementation is provided
  * by the `step`.
  *
  * @param graph The graph into which this node is to be inserted.
  * @param step An optional implementation step.
  * @param userLabel An optional user-defined label.
  */
class AtomicNode protected[jafpl] (override val graph: Graph,
                                   override val step: Option[Step],
                                   override val userLabel: Option[String])
  extends Node(graph,step,userLabel) {

  private[graph] override def inputsOk() = true
  private[graph] override def outputsOk() = true
}
