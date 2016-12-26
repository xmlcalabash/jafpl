package com.jafpl.graph

import com.jafpl.runtime.{CompoundStep, DefaultCompoundStart}

/**
  * Created by ndw on 10/2/16.
  */
class CatchStart(graph: Graph, step: Option[CompoundStep], nodes: List[Node]) extends DefaultCompoundStart(graph, step, nodes) {
  label = Some("_catch_start")
  private var cexcept: Option[Throwable] = None

  def caughtException: Boolean = {
    cexcept.isDefined
  }

  override def caught(exception: Throwable): Boolean = {
    cexcept = Some(exception)
    true
  }
}
