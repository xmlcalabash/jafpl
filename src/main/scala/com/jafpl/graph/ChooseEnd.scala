package com.jafpl.graph

import com.jafpl.runtime.{CompoundStep, DefaultCompoundEnd}

/**
  * Created by ndw on 10/2/16.
  */
class ChooseEnd(graph: Graph, step: Option[CompoundStep]) extends DefaultCompoundEnd(graph, step) {
  label = Some("_choose_end")
}
