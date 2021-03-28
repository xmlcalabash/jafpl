package com.jafpl.runtime

import com.jafpl.graph.BindingParams
import com.jafpl.messages.Message

/** Evaluate expressions (for variable bindings and the alternatives in a choose).
 *
 * The expression evaluator is responsible for taking the string form of an expression and evaluating it.
 * The runtime is agnostic to the form of the expressions or their results. In order to support choose
 * steps, it's necessary for the evaluator to be able to return a boolean result for an expression.
 *
 * The evaluator may throw a [[com.jafpl.exceptions.JafplException]] if the specified expression is
 * invalid or, in the case of a request for a boolean, if it has no boolean value.
 *
 */
trait ExpressionEvaluator {
  /** Obtain a new instance of the expression evaluator.
   *
   * If the evalauator is reentrant then it can simply return itself. However, if it has local state,
   * then it must return a new instance ready to evaluate an expression.
   *
   * @return An instance of itself.
   */
  def newInstance(): ExpressionEvaluator

  def setContextItem(message: Message): Unit
  def setContextItem(messages: List[Message]): Unit
  def setContextCollection(message: List[Message]): Unit

  /** Evaluate an expression. The expression may return any number of results, including none.
   *
   * The `expr` is evaluated according to whatever grammar the evaluator supports. The context is
   * an item from the pipeline. The bindings are variable bindings from the pipeline.
   *
   * @param expr     The expression to evaluate.
   * @param bindings Any variable bindings that are provided for the expression.
   * @return The computed value of the expression.
   */
  def value(expr: Any, bindings: Map[String, Message], params: Option[BindingParams]): Message

  /** Evaluate an expression that is expected to return a single value.
   *
   * The `expr` is evaluated according to whatever grammar the evaluator supports. The context is
   * an item from the pipeline. The bindings are variable bindings from the pipeline.
   *
   * @param expr     The expression to evaluate.
   * @param bindings Any variable bindings that are provided for the expression.
   * @return The computed value of the expression.
   */
  def singletonValue(expr: Any, bindings: Map[String, Message], params: Option[BindingParams]): Message

  /**
   * Evaluate an expression and cast the result to a boolean.
   *
   * @param expr     The expression to evaluate.
   * @param bindings Any variable bindings that are provided for the expression.
   * @return The boolean value of the computed expression.
   */
  def booleanValue(expr: Any, bindings: Map[String, Message], params: Option[BindingParams]): Boolean
}
