package com.jafpl.graph

private[jafpl] class Binding(override val graph: Graph,
                             val name: String,
                             val expression: String) extends Node(graph,None,None) {
  private var _start: Option[ContainerStart] = None

  private val _label = s"${name}-$id"

  override def label: String = _label

  override def toString: String = {
    s"{$label}"
  }

  override def inputsOk(): Boolean = {
    inputs.isEmpty || ((inputs.size == 1) && inputs.contains("source"))
  }

  override def outputsOk(): Boolean = {
    outputs.isEmpty
  }
}