package com.jafpl.util

import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

object DefaultTraceEventManager {
  val ALL = "ALL"
}

class DefaultTraceEventManager() extends TraceEventManager {
  protected[jafpl] val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected val enabledTraces = mutable.HashSet.empty[String]
  protected val disabledTraces = mutable.HashSet.empty[String]

  private val prop = Option(System.getProperty("com.xmlcalabash.trace"))
  if (prop.isDefined) {
    for (trace <- prop.get.split(",")) {
      var event = trace.trim()
      var enable = true

      if (trace.startsWith("-")) {
        event = trace.substring(1)
        enable = false
      } else {
        if (trace.startsWith("+")) {
          event = trace.substring(1)
        }
      }

      if (enable) {
        enableTrace(event)
      } else {
        disableTrace(event)
      }
    }
  }

  override def enableTrace(event: String): Unit = {
    enabledTraces += event.toLowerCase()
    disabledTraces -= event.toLowerCase()
  }

  override def disableTrace(event: String): Unit = {
    disabledTraces += event.toLowerCase()
    enabledTraces -= event.toLowerCase()
  }

  override def traceEnabled(event: String): Boolean = {
    val lcevent = event.toLowerCase()
    enabledTraces.contains(lcevent) || (enabledTraces.contains(DefaultTraceEventManager.ALL) && !disabledTraces.contains(lcevent))
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
