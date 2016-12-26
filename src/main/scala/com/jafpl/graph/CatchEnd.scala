package com.jafpl.graph

import com.jafpl.runtime.{CompoundStep, DefaultCompoundEnd}

/**
  * Created by ndw on 10/2/16.
  */
class CatchEnd(graph: Graph, step: Option[CompoundStep]) extends DefaultCompoundEnd(graph, step) {
  label = Some("_catch_end")
}
