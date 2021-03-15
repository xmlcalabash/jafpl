package com.jafpl.runtime

/** The runtime configuration for pipeline execution.
 *
 * This object provides configuration information to the pipeline execution engine.
 *
 */
trait RuntimeConfiguration {
  /** Returns the expression evaluator.
   *
   * An [[ExpressionEvaluator]] is required to evaluate expressions
   * for variable bindings and the when branches of a choose.
   *
   * @return The evaluator.
   */
  def expressionEvaluator: ExpressionEvaluator

  /** Enable trace events.
   *
   * The actors that run steps will emit log messages if the appropriate traces are enabled.
   * This method is called to determine if a particular trace is enabled.
   *
   * @param trace A trace event.
   * @return True, if that event should be considered enabled.
   */
  def traceEnabled(trace: String): Boolean

  /** Thread pool size
   *
   * The number of parallel threads that may be used to execute steps. The actual number of
   * threads used will depend on the degree of parallelism in the pipeline, but will never
   * exceed this limit.
   *
   * @return The thread pool size
   */
  def threadPoolSize: Int
}
