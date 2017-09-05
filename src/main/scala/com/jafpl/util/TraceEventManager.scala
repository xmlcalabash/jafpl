package com.jafpl.util

trait TraceEventManager {
  def enableTrace(event: String): Unit
  def disableTrace(event: String): Unit
  def traceEnabled(event: String): Boolean
  def trace(message: String, event: String): Unit
  def trace(level: String, message: String, event: String): Unit
}
