package com.jafpl.graph

// FIXME: Exposing Binding is abstration leakage

/** A variable binding.
  *
  * @param graph The graph into which this node is to be inserted.
  * @param name The variable's name
  * @param expression It's initializer expression
  */
class Binding(override val graph: Graph,
              val name: String,
              val expression: Option[String]) extends Node(graph,None,None) {
  private var _start: Option[ContainerStart] = None
  private val _label = s"${name}-$id"

  def this(graph: Graph, name: String) {
    this(graph, name, None)
  }

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
