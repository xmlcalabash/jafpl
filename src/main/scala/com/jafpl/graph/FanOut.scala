package com.jafpl.graph

import com.jafpl.runtime.Step

/**
  * Created by ndw on 10/3/16.
  */
private[graph] class FanOut(graph: Graph, step: Step) extends Node(graph, Some(step)) {
  var portCount = 0

  def this(graph: Graph) {
    this(graph, new Fan("_fan_out"))
  }

  def nextPort: Port = {
    portCount += 1
    new Port(this, "result_" + portCount.toString)
  }
}
