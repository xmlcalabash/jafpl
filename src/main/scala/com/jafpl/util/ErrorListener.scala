package com.jafpl.util

import com.jafpl.graph.Location

trait ErrorListener {
  def error(message: String, location: Option[Location]): Unit
  def error(cause: Throwable, location: Option[Location]): Unit
  def error(cause: Throwable): Unit
  def warning(message: String, location: Option[Location]): Unit
  def warning(cause: Throwable, location: Option[Location]): Unit
  def warning(cause: Throwable): Unit
  def info(message: String, location: Option[Location]): Unit
}
