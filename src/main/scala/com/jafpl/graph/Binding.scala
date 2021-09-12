package com.jafpl.graph

import com.jafpl.steps.{ManifoldSpecification, PortSpecification}

/** A variable binding.
  *
  * When pipelines are constructed, variable bindings associate expressions with variable names.
  * At runtime, the association is between the name and its computed value.
  *
  * @constructor Use the `addBinding()` method on [[com.jafpl.graph.ContainerStart]] to construct a binding.
  * @param graph The graph into which this node is to be inserted.
  * @param name The variable's name
  * @param expression Its initializer expression
  */
class Binding protected[jafpl] (override val graph: Graph,
                                val name: String,
                                val expression: Any,
                                val params: Option[BindingParams]) extends AtomicNode(graph, None, None) with ManifoldSpecification {

  protected var _start: Option[ContainerStart] = None
  protected val _label = s"$name-$id"

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

  def bindingFor: Node = {
    // This method assumes the graph is valid
    // There will be only one outbound edge
    val edge = graph.edgesFrom(this).head
    // It will go to a Joiner or to the node the binding is for
    var to = edge.to
    while (to.isInstanceOf[Joiner]) {
      to = to.graph.edgesFrom(to).head.to
    }

    to
  }

  private[graph] override def inputsOk(): Boolean = {
    var valid = true
    for (port <- inputs) {
      if (!validPortName(port, "source")) {
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

  def inputSpec: PortSpecification = PortSpecification.ANY
  def outputSpec: PortSpecification = PortSpecification.RESULT
  override def manifold: Option[ManifoldSpecification] = Some(this)
}
