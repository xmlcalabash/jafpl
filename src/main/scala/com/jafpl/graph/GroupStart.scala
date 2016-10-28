package com.jafpl.graph

import com.jafpl.runtime.{CompoundStep, DefaultCompoundStart}

/**
  * Created by ndw on 10/2/16.
  */
class GroupStart(graph: Graph, step: Option[CompoundStep], nodes: List[Node]) extends DefaultCompoundStart(graph, step, nodes) {
  label = Some("_group_start")

  override private[graph] def run(): Unit = {
    for (output <- outputs) {
      close(output)
    }
  }
}
