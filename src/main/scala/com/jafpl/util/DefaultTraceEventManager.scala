package com.jafpl.util

import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class DefaultTraceEventManager() extends TraceEventManager {
  protected[jafpl] val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected val traces = mutable.HashSet.empty[String]

  override def enableTrace(event: String): Unit = {
    traces += event
  }

  override def disableTrace(event: String): Unit = {
    traces -= event
  }

  override def traceEnabled(event: String): Boolean = {
    traces.contains(event)
  }

  override def trace(message: String, event: String): Unit = {
    trace("info", message, event)
  }

  override def trace(level: String, message: String, event: String): Unit = {
    if (traceEnabled(event)) {
      level match {
        case "debug" => logger.debug(message)
        case "warn" => logger.warn(message)
        case "error" => logger.error(message)
        case _ => logger.info(message)
      }
    }
  }
}
