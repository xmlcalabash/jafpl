package com.jafpl.graph

import com.jafpl.graph.GraphMonitor.GSelectWhen
import com.jafpl.runtime.{Chooser, CompoundStep, DefaultCompoundStart}

/**
  * Created by ndw on 10/2/16.
  */
class ChooseStart(graph: Graph, step: Option[CompoundStep], nodes: List[Node]) extends DefaultCompoundStart(graph, step, nodes) {
  private var cachePort = 1
  private var _chosenWhen: Option[Node] = None
  label = Some("_choose_start")

  def chosenWhen = _chosenWhen

  override private[graph] def run(): Unit = {
    _chosenWhen = Some(step.get.asInstanceOf[Chooser].pickOne(nodes))
    graph.monitor ! GSelectWhen(this, _chosenWhen.get)
  }

  override private[graph] def addChooseCaches(): Unit = {
    for (child <- nodes) {
      child match {
        case when: WhenStart =>
          for (input <- child.inputs) {
            val edge = child.input(input).get
            if (edge.inputPort == "condition") {
              logger.debug("Choose caches: " + edge)
              val portName = "choose_" + cachePort
              graph.removeEdge(edge)
              graph.addEdge(edge.source, edge.outputPort, this, "I_" + portName)
              graph.addEdge(this, "O_" + portName, edge.destination, edge.inputPort)
              cachePort += 1
            }
          }
        case _ => Unit
      }
    }

    for (child <- nodes) {
      child.addChooseCaches()
    }
  }
}
