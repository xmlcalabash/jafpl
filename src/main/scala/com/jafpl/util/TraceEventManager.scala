package com.jafpl.util

object TraceEventManager {
  val ALL = "all"
  val GRAPH = "graph"
  val SCHEDULER = "scheduler"
  val THREADS = "threads"
  val CREATE = "create"
  val INIT = "init"
  val START = "start"
  val RUN = "run"
  val CARDINALITY = "cardinality"
  val RECEIVE = "receive"
  val BINDING = "binding"
  val RESET = "reset"
  val ABORT = "abort"
  val STOP = "stop"
  val LOOP = "loop"
  val LISTEN = "listen"
  val MESSAGES = "messages"
  val CHOOSE = "choose"
  val UNTIL = "until"
  val WHILE = "while"
  val IO = "io"
  val EXCEPTIONS = "exceptions"
  val MUTEX = "mutex"
}

trait TraceEventManager {
  def enableTrace(event: String): Unit
  def disableTrace(event: String): Unit
  def traceEnabled(event: String): Boolean
  def traceExplicitlyEnabled(event: String): Boolean
  def trace(message: String, event: String): Unit
  def trace(level: String, message: String, event: String): Unit
}
