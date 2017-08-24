package com.jafpl.steps

import com.jafpl.exceptions.{GraphException, PipelineException}

import scala.collection.{immutable, mutable}

/** Useful default binding specifications.
  *
  */
object BindingSpecification {
  /** Allow any bindings. */
  val ANY: BindingSpecification = new BindingSpecification(Set())
}

/** Specify the required bindings for steps.
  *
  * The core engine doesn't care what bindings are passed to steps. If you have a step
  * that requires a specific set of bindings, you can declare that explicitly. Once declared
  * the pipeline engine will assure that a binding is provided.
  *
  * Unlike ports, which have cardinalities, all that you can specify for bindings is that
  * they are required. You cannot forbid bindings; steps must ignore "extra" bindings
  * that might be provided by the pipeline.
  *
  * @constructor A binding specification.
  * @param bindings A set of the names of required bindings.
  */
class BindingSpecification(val bindings: immutable.Set[String]) {
}
