package com.jafpl.graph

import com.jafpl.graph.GraphMonitor.GSubgraph
import com.jafpl.runtime.CompoundStep
import com.jafpl.util.TreeWriter
import net.sf.saxon.s9api.QName

/**
  * Created by ndw on 10/2/16.
  */
class WhenStart(graph: Graph, name: Option[String], step: Option[CompoundStep], nodes: List[Node]) extends Node(graph, name, step) with CompoundStart {
  var _whenEnd: WhenEnd = _
  var cachePort = 1

  def endNode = _whenEnd
  private[graph] def endNode_=(node: WhenEnd): Unit = {
    _whenEnd = node
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

  override private[graph] def addWhenCaches(when: Option[WhenStart]): Unit = {
    /*
    for (child <- nodes) {
      child.addWhenCaches(Some(this))
    }
    */

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
          logger.debug("Add when cache: " + edge)
          val portName = "cache_" + cachePort
          graph.removeEdge(edge)
          graph.addEdge(edge.source, edge.outputPort, this, "I_" + portName)
          graph.addEdge(this, "O_" + portName, edge.destination, edge.inputPort)
          cachePort += 1
        }
      }
    }
  }

  override def dumpExtraAttr(tree: TreeWriter): Unit = {
    tree.addAttribute(Serializer._compound_end, _whenEnd.uid.toString)
    var nodeList = ""
    for (node <- nodes) {
      nodeList += node.uid.toString + " "
    }
    tree.addAttribute(Serializer._compound_children, nodeList)
  }
}
