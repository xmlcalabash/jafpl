package com.jafpl.graph

import com.jafpl.exceptions.GraphException

private[jafpl] class ContainerEnd(override val graph: Graph) extends Node(graph, None, None) {
  var _start: Option[ContainerStart] = None

  override def inputsOk() = true
  override def outputsOk() = true

  def start: Option[ContainerStart] = _start
  def start_=(node: ContainerStart): Unit = {
    if (_start.isEmpty) {
      if (node.containerEnd != this) {
        throw new GraphException("End of " + this + " is this: " + node, location)
      }
      _start = Some(node)
    } else {
      throw new GraphException("Start of " + this + " is already defined: " + _start.get, location)
    }
  }

}
