package com.jafpl.graph

import com.jafpl.runtime.Step
import com.jafpl.util.UniqueId

/**
  * Created by ndw on 10/3/16.
  */
private[graph] class FanIn(graph: Graph, step: Step) extends Node(graph, Some(step)) {
  var portCount = 0

  def this(graph: Graph) {
    this(graph, new Fan("_fan_in"))
  }

  def nextPort: Port = {
    portCount += 1
    new Port(this, "source_" + portCount.toString)
  }
}
