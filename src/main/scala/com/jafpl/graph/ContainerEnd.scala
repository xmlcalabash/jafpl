package com.jafpl.graph

import com.jafpl.exceptions.JafplException

private[jafpl] class ContainerEnd(override val graph: Graph) extends Node(graph, None, None) {
  private var _start: Option[ContainerStart] = None

  override def inputsOk() = true
  override def outputsOk() = true

  def start: Option[ContainerStart] = _start
  def start_=(node: ContainerStart): Unit = {
    if (_start.isEmpty) {
      if (node.containerEnd != this) {
        throw JafplException.badContainerEnd(this.toString, node.toString, location)
      }
      _start = Some(node)
      internal_name = node.internal_name + "_end"
    } else {
      throw JafplException.dupContainerStart(this.toString, _start.get.toString, location)
    }
  }

}
