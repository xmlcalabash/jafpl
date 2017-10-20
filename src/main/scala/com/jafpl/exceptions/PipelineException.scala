package com.jafpl.exceptions

import com.jafpl.graph.Location

object PipelineException {
  def INTERNALERR(msg: String, location: Option[Location]): PipelineException = new PipelineException("INTERNALERR", msg, location)
  def BADMESSAGE(msg: String, location: Option[Location]): PipelineException = new PipelineException("BADMESSAGE", msg, location)
}

/** An exception raised during pipeline evaluation.
  *
  * @constructor A pipeline exception.
  * @param code An error code (can be caught by catch steps).
  */
class PipelineException(val code: Any) extends Throwable {
  protected var _message = Option.empty[String]
  protected var _location = Option.empty[Location]
  protected var _cause = Option.empty[Throwable]

  /** The exception message, if there was one. */
  def message: Option[String] = _message

  /** The location, if there was one. */
  def location: Option[Location] = _location

  /** The cause, if this exception wraps a deeper exception. */
  def cause: Option[Throwable] = _cause

  /** String representation */
  override def toString: String = {
    "PipelineException(" + code + "," + message.getOrElse("???") + ")"
  }

  /** Alternate constructor with no location, cause or data. */
  def this(code: Any, message: String) {
    this(code)
    _message = Some(message)
  }

  /** Alternate constructor with no message, cause or data. */
  def this(code: Any, location: Option[Location]) {
    this(code)
    _location = location
  }

  /** Alternate constructor with no cause or data. */
  def this(code: String, message: String, location: Option[Location]) {
    this(code)
    _message = Some(message)
    _location = location
  }

  /** Alternate constructor with no cause or data. */
  def this(code: String, message: String, location: Location) {
    this(code)
    _message = Some(message)
    _location = Some(location)
  }

  /** Alternate constructor with no data. */
  def this(code: String, message: String, location: Location, cause: Throwable) {
    this(code)
    _message = Some(message)
    _location = Some(location)
    _cause = Some(cause)
  }
}
