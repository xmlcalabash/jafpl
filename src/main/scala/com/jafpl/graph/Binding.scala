package com.jafpl.graph

/** A variable binding.
  *
  * When pipelines are constructed, variable bindings associate expressions with variable names.
  * At runtime, the association is between the name and its computed value.
  *
  * Variables are lexically scoped.
  *
  * @constructor Use the `addBinding()` method on [[com.jafpl.graph.ContainerStart]] to construct a binding.
  * @param graph The graph into which this node is to be inserted.
  * @param name The variable's name
  * @param expression Its initializer expression
  */
class Binding protected[jafpl] (override val graph: Graph,
                                val name: String,
                                val expression: Option[String]) extends Node(graph,None,None) {
  private var _start: Option[ContainerStart] = None
  private val _label = s"${name}-$id"

  protected[jafpl] def this(graph: Graph, name: String) {
    this(graph, name, None)
  }

  override def label: String = _label

  override def toString: String = {
    s"{$label}"
  }

  private[graph] override def inputsOk(): Boolean = {
    inputs.isEmpty || ((inputs.size == 1) && inputs.contains("source"))
  }

  private[graph] override def outputsOk(): Boolean = {
    outputs.isEmpty
  }
}
