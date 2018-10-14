package com.jafpl.runtime

import akka.actor.Actor
import akka.event.Logging
import com.jafpl.runtime.TraceEvent.TraceEvent

object TraceEvent extends Enumeration {
  type TraceEvent = Value
  val METHODS, NMESSAGES, GMESSAGES, STEPIO, CARDINALITY, BINDINGS, TRACES, WATCHDOG = Value
}

abstract class TracingActor(protected val runtime: GraphRuntime) extends Actor {
  protected val log = Logging(context.system, this)

  protected def trace(code: String, details: String, event: TraceEvent): Unit = {
    trace("info", code, details, event)
  }

  protected def trace(level: String, code: String, details: String, event: TraceEvent): Unit = {
    val message = traceMessage(code, details)
    // We don't use the traceEventManager.trace() call because we want to use the Akka logger
    if (runtime.traceEventManager.traceEnabled(event.toString.toLowerCase())) {
      level match {
        case "info" => log.info(message)
        case "debug" => log.debug(message)
        case _ => log.warning(message)
      }
    }
  }

  protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details
  }
}
