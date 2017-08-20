package com.jafpl.exceptions

/** An exception raised during pipeline evaluation.
  *
  * @constructor A pipeline exception.
  * @param code An error code (can be caught by catch steps).
  * @param message An explanatory message.
  * @param cause The underlying exception, if there was one.
  * @param data Arbitrary data that the step would like to communicate to a catch.
  */
class PipelineException(val code: String, val message: String, val cause: Option[Throwable], val data: Option[Any])
  extends Throwable {

  /** String representation */
  override def toString: String = {
    "PipelineException(" + code + "," + message + ")"
  }

  /** Alternate constructor with no cause or data. */
  def this(code: String, message: String) {
    this(code, message, None, None)
  }

  /** Alternate constructor with no data. */
  def this(code: String, message: String, cause: Throwable) {
    this(code, message, Some(cause), None)
  }

  /** Alternate constructor with an explicit cause and data rather than optional ones. */
  def this(code: String, message: String, cause: Throwable, data: Any) {
    this(code, message, Some(cause), Some(data))
  }
}
