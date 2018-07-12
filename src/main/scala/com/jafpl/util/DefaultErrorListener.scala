package com.jafpl.util

import com.jafpl.exceptions.{JafplException, JafplExceptionCode}
import com.jafpl.graph.Location

class DefaultErrorListener extends ErrorListener {
  override def error(message: String, location: Option[Location]): Unit = {
    msg("error", message, location)
  }

  override def error(cause: Throwable, location: Option[Location]): Unit = {
    msg("error", cause.getMessage, location)
  }

  override def error(cause: Throwable): Unit = {
    error(cause, None)
  }

  override def warning(message: String, location: Option[Location]): Unit = {
    msg("warn", message, location)
  }

  override def warning(cause: Throwable, location: Option[Location]): Unit = {
    msg("warn", cause.getMessage, location)
  }

  override def warning(cause: Throwable): Unit = {
    warning(cause, None)
  }

  override def info(message: String, location: Option[Location]): Unit = {
    msg("info", message, location)
  }

  private def msg(level: String, message: String, location: Option[Location]): Unit = {
    var line = ""

    line += level
    if (location.isDefined) {
      val loc = location.get
      if (loc.uri.isDefined) {
        line += ":" + loc.uri.get
      }
      if (loc.line.isDefined) {
        line += ":" + loc.line.get
      }
      if (loc.column.isDefined) {
        line += ":" + loc.column.get
      }
    }
    line += ":" + message

    println(line)
  }
}
