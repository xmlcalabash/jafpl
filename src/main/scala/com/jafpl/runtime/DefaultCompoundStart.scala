package com.jafpl.runtime

import com.jafpl.graph.GraphMonitor.GSubgraph
import com.jafpl.graph.{CompoundStart, Graph, Node, Serializer}
import com.jafpl.util.XmlWriter

/**
  * Created by ndw on 10/16/16.
  */
abstract class DefaultCompoundStart(graph: Graph, step: Option[CompoundStep], nodes: List[Node]) extends Node(graph, step) with CompoundStart {
  private var _end: Node = _

  def compoundEnd = _end
  private[jafpl] def compoundEnd_=(node: Node): Unit = {
    _end = node
  }

  final def runAgain: Boolean = {
    if (step.isDefined) {
      step.get.runAgain
    } else {
      false
    }
  }

  def subpipeline = nodes

  override private[jafpl] def makeActors(): Unit = {
    super.makeActors()
  }

  override private[jafpl] def identifySubgraphs(): Unit = {
    super.identifySubgraphs()
    graph.monitor ! GSubgraph(this, nodes)
  }

  override def dumpExtraAttr(tree: XmlWriter): Unit = {
    // This is a hack; I expect all of the compoundEnds to be extensions of Node
    if (_end.isInstanceOf[Node]) {
      tree.addAttribute(Serializer._compound_end, _end.asInstanceOf[Node].uid.toString)
    }
    var nodeList = ""
    for (node <- nodes) {
      nodeList += node.uid.toString + " "
    }
    tree.addAttribute(Serializer._compound_children, nodeList)
  }
}
