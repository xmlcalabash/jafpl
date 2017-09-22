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
                                val expression: Option[Any],
                                val options: Option[Any])
  extends Node(graph, None, None) {

  private var _start: Option[ContainerStart] = None
  private val _label = s"$name-$id"

  def this(graph: Graph, name: String) = {
    this(graph, name, None, None)
  }

  override def label: String = _label

  override def toString: String = {
    s"{$label}"
  }

  override def outputs: Set[String] = {
    if (super.outputs.isEmpty) {
      Set("result")
    } else {
      super.outputs
    }
  }

  private[graph] override def inputsOk(): Boolean = {
    var valid = true
    for (port <- inputs) {
      if ((port != "#bindings") && (port != "source")) {
        valid = false
        logger.error(s"Invalid input binding on variable: $port")
      }
    }
    valid
  }

  private[graph] override def outputsOk(): Boolean = {
    var valid = true
    for (port <- outputs) {
      if (port != "result") {
        valid = false
        logger.error(s"Invalid output binding on variable: $port")
      }
    }
    valid
  }
}
