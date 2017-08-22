package com.jafpl.exceptions

import com.jafpl.graph.Location

/** An exception raised during pipeline evaluation.
  *
  * @constructor A pipeline exception.
  * @param code An error code (can be caught by catch steps).
  * @param message An explanatory message.
  * @param location The proximate location of the cause of the exception
  * @param cause The underlying exception, if there was one.
  * @param data Arbitrary data that the step would like to communicate to a catch.
  */
class PipelineException(val code: String,
                        val message: String,
                        val location: Option[Location],
                        val cause: Option[Throwable],
                        val data: Option[Any])
  extends Throwable {

  /** String representation */
  override def toString: String = {
    "PipelineException(" + code + "," + message + ")"
  }

  /** Alternate constructor with no location, cause, or data. */
  def this(code: String, message: String) {
    this(code, message, None, None, None)
  }

  /** Alternate constructor with no cause or data. */
  def this(code: String, message: String, location: Location) {
    this(code, message, Some(location), None, None)
  }

  /** Alternate constructor with no data. */
  def this(code: String, message: String, location: Location, cause: Throwable) {
    this(code, message, Some(location), Some(cause), None)
  }
}
