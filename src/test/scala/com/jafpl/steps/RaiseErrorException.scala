package com.jafpl.steps

import com.jafpl.exceptions.JafplExceptionCode
import com.jafpl.graph.Location

class RaiseErrorException(val code: String, val message: String, val location: Option[Location]) extends Throwable with JafplExceptionCode {
  override def jafplExceptionCode: Any = code
}
