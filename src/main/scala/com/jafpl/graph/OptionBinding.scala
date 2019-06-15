package com.jafpl.graph

/** An option binding.
  *
  * When pipelines are constructed, option bindings associate expressions with variable names.
  * At runtime, the association is between the name and its computed value.
  *
  * An [[OptionBinding]] is just like a [[Binding]] except that a specific, computed value may be
  * provided before the graph begins executing. In that case, the provided value is used and the
  * expression is not evaluated.
  *
  * @constructor Use the `addBinding()` method on [[com.jafpl.graph.ContainerStart]] to construct a binding.
  * @param graph The graph into which this node is to be inserted.
  * @param name The variable's name
  * @param expression Its initializer expression
  */
class OptionBinding protected[jafpl](override val graph: Graph,
                                     override val name: String,
                                     override val expression: Any,
                                     override val static: Boolean,
                                     override val options: Option[Any])
  extends Binding(graph, name, expression, static, options) {

  var _value: Option[Any] = None

  def value: Option[Any] = _value
  def value_=(value: Any): Unit = {
    _value = Some(value)
  }
}
