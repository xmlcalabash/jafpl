package com.jafpl.graph

import com.jafpl.graph.GraphMonitor.GSubgraph
import com.jafpl.runtime.CompoundStep
import com.jafpl.util.TreeWriter

/**
  * Created by ndw on 10/2/16.
  */
class ChooseStart(graph: Graph, name: Option[String], step: Option[CompoundStep], nodes: List[Node]) extends Node(graph, name, step) with CompoundStart {
  var _chooseEnd: ChooseEnd = _

  def endNode = _chooseEnd
  private[graph] def endNode_=(node: ChooseEnd): Unit = {
    _chooseEnd = node
  }

  final def runAgain = false

  private[graph] def subpipeline = nodes

  override private[graph] def makeActors(): Unit = {
    val made = madeActors

    super.makeActors()

    if (!made) {
      graph.monitor ! GSubgraph(_actor, nodes)
    }
  }

  override def dumpExtraAttr(tree: TreeWriter): Unit = {
    tree.addAttribute(Serializer._compound_end, _chooseEnd.uid.toString)
    var nodeList = ""
    for (node <- nodes) {
      nodeList += node.uid.toString + " "
    }
    tree.addAttribute(Serializer._compound_children, nodeList)
  }
}
