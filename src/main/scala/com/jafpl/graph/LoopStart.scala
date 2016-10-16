package com.jafpl.graph

import com.jafpl.runtime.{CompoundStep, DefaultCompoundStart}

/**
  * Created by ndw on 10/2/16.
  */
class LoopStart(graph: Graph, step: Option[CompoundStep], nodes: List[Node]) extends DefaultCompoundStart(graph, step, nodes) {
  label = Some("_loop_start")

  override def addIterationCaches(): Unit = {
    for (child <- nodes) {
      child.addIterationCaches()
    }

    for (child <- nodes) {
      for (input <- child.inputs()) {
        val edge = child.input(input).get
        val node = edge.source
        var found = (node == this)
        for (cnode <- nodes) {
          found = found || node == cnode
        }
        if (!found) {
          // Cache me Amadeus
          val cache = graph.createIterationCacheNode()
          graph.removeEdge(edge)
          graph.addEdge(edge.source, edge.outputPort, cache, "source")
          graph.addEdge(cache, "result", edge.destination, edge.inputPort)
        }
      }
    }
  }
}
