package com.jafpl.graph

import com.jafpl.graph.GraphMonitor.GSubgraph
import com.jafpl.runtime.{Chooser, CompoundStep}
import com.jafpl.util.XmlWriter

/**
  * Created by ndw on 10/2/16.
  */
class ChooseStart(graph: Graph, step: Option[CompoundStep], nodes: List[Node]) extends Node(graph, step) with CompoundStart {
  var _chooseEnd: ChooseEnd = _
  var cachePort = 1
  label = Some("_choose_start")

  def endNode = _chooseEnd
  private[graph] def endNode_=(node: ChooseEnd): Unit = {
    _chooseEnd = node
  }

  final def runAgain = false

  def subpipeline = nodes

  override private[graph] def run(): Unit = {
    step.get.asInstanceOf[Chooser].pickOne(nodes)
  }

  override private[graph] def makeActors(): Unit = {
    super.makeActors()
    graph.monitor ! GSubgraph(_actor, nodes)
  }

  override private[graph] def addWhenCaches(when: Option[WhenStart]): Unit = {
    /*
    for (child <- nodes) {
      child.addChooseCaches(Some(this))
    }
    */

    for (child <- nodes) {
      child match {
        case when: WhenStart =>
          for (input <- child.inputs()) {
            val edge = child.input(input).get
            if (edge.inputPort == "condition") {
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
  }

  override def dumpExtraAttr(tree: XmlWriter): Unit = {
    tree.addAttribute(Serializer._compound_end, _chooseEnd.uid.toString)
    var nodeList = ""
    for (node <- nodes) {
      nodeList += node.uid.toString + " "
    }
    tree.addAttribute(Serializer._compound_children, nodeList)
  }
}
