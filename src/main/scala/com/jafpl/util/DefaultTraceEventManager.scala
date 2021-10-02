package com.jafpl.util

import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable

class DefaultTraceEventManager() extends TraceEventManager {
  protected[jafpl] val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected val enabledTraces = mutable.HashSet.empty[String]
  protected val disabledTraces = mutable.HashSet.empty[String]

  private val prop = Option(System.getProperty("com.jafpl.trace"))
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
    enabledTraces.contains(lcevent) || (enabledTraces.contains("all") && !disabledTraces.contains(lcevent))
  }

  override def traceExplicitlyEnabled(event: String): Boolean = {
    val lcevent = event.toLowerCase()
    enabledTraces.contains(lcevent)
  }

  override def trace(message: String, event: String): Unit = {
    trace("info", message, event)
  }

  override def trace(level: String, message: String, event: String): Unit = {
    val msg = if (traceEnabled("threads")) {
      val id = Thread.currentThread().getId
      f"$id%4d $message%s"
    } else {
      message
    }

    if (traceEnabled(event)) {
      level match {
        case "debug" => logger.debug(msg)
        case "warn" => logger.warn(msg)
        case "error" => logger.error(msg)
        case _ => logger.info(msg)
      }
    }
  }
}
