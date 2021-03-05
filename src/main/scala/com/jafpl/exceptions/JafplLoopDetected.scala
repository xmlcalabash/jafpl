package com.jafpl.exceptions

import com.jafpl.graph.{AtomicNode, Binding, Graph, Joiner, Location, Node, Splitter}

import scala.collection.mutable.ListBuffer

class JafplLoopDetected(location: Option[Location]) extends JafplException(JafplException.LOOP_IN_GRAPH, location, List.empty[Any]) {
  private val _nodes = ListBuffer.empty[Node]

  protected[jafpl] def addNode(step: Node): Unit = {
    // Don't report splitters and joiners; they're not useful to the pipeline author
    step match {
      case join: Joiner => ()
      case split: Splitter => ()
      case _ => _nodes += step

    }
  }

  def nodes: List[Node] = _nodes.toList

  override def toString: String = {
    var loop = "Loop detected:"
    var arrow = " "
    for (pnode <- _nodes) {
      loop = loop + arrow + pnode
      arrow = "→"
    }
    loop
  }
}
