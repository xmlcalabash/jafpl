package com.jafpl.graph

import com.jafpl.runtime.{CompoundStep, DefaultCompoundStart}

/**
  * Created by ndw on 10/2/16.
  */
class WhenStart(graph: Graph, step: Option[CompoundStep], nodes: List[Node]) extends DefaultCompoundStart(graph, step, nodes) {
  private var cachePort = 1
  label = Some("_when_start")

  override private[graph] def addWhenCaches(): Unit = {
    for (child <- nodes) {
      for (input <- child.inputs()) {
        val edge = child.input(input).get
        val node = edge.source
        var found = (node == this)
        for (cnode <- nodes) {
          found = found || node == cnode
        }
        if (!found) {
          logger.debug("When caches: " + edge)
          val portName = "when_" + cachePort
          graph.removeEdge(edge)
          graph.addEdge(edge.source, edge.outputPort, this, "I_" + portName)
          graph.addEdge(this, "O_" + portName, edge.destination, edge.inputPort)
          cachePort += 1
        }
      }
    }

    for (child <- nodes) {
      child.addWhenCaches()
    }
  }
}
