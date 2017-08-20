package com.jafpl.runtime

/** Evaluate expressions (for variable bindings and the alternatives in a choose).
  *
  * The expression evaluator is responsible for taking the string form of an expression and evaluating it.
  * The runtime is agnostic to the form of the expressions or their results. In order to support choose
  * steps, it's necessary for the evaluator to be able to return a boolean result for an expression.
  *
  * The evaluator may throw a [[com.jafpl.exceptions.PipelineException]] if the specified expression is
  * invalid or, in the case of a request for a boolean, if it has no boolean value.
  *
  */
trait ExpressionEvaluator {
  /** Evaluate an expression.
    *
    * The `expr` is evaluated according to whatever grammar the evaluator supports. The context is
    * an item from the pipeline. The bindings are variable bindings from the pipeline.
    *
    * @param expr The expression to evaluate.
    * @param context An optional, single item that is flowing through the pipeline.
    * @param bindings Any variable bindings that are provided for the expression.
    * @return The computed value of the expression.
    */
  def value(expr: String, context: Option[Any], bindings: Option[Map[String,Any]]): Any

  /**
    * Evaluate an expression and cast the result to a boolean.
    *
    * @param expr The expression to evaluate.
    * @param context An optional, single item that is flowing through the pipeline.
    * @param bindings Any variable bindings that are provided for the expression.
    * @return The boolean value of the computed expression.
    */
  def booleanValue(expr: String, context: Option[Any], bindings: Option[Map[String,Any]]): Boolean
}
