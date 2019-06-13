package com.jafpl.exceptions

import com.jafpl.graph.{AtomicNode, Binding, Graph, Joiner, Location, Node, Splitter}

import scala.collection.mutable.ListBuffer

class JafplLoopDetected(graph: Graph, location: Option[Location]) extends JafplException(JafplException.LOOP_IN_GRAPH, location, List.empty[Any]) {
  private val _nodes = ListBuffer.empty[LoopNode]

  protected[jafpl] def addNode(step: Node): Unit = {
    step match {
      case atomic: AtomicNode =>
        _nodes += new LoopNode(atomic.userLabel.getOrElse("ANONYMOUS"), atomic.step.get.location)
      case join: Joiner => Unit
      case split: Splitter => Unit
      case bind: Binding =>
        _nodes += new LoopNode(bind.name, None)
      case _ => print("unknown", step)
    }
  }

  def nodes: List[LoopNode] = _nodes.toList

  override def toString: String = {
    var loop = ""
    var arrow = ""
    for (pnode <- _nodes) {
      loop = loop + arrow + pnode
      arrow = "→"
    }
    loop
  }

  class LoopNode(val label: String, val location: Option[Location]) {
    // nop
  }
}
