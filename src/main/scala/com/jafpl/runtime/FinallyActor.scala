package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.ContainerStart
import com.jafpl.messages.ExceptionMessage
import com.jafpl.runtime.GraphMonitor.{GClose, GOutput, GStart}

private[runtime] class FinallyActor(private val monitor: ActorRef,
                                    override protected val runtime: GraphRuntime,
                                    override protected val node: ContainerStart) extends StartActor(monitor, runtime, node)  {
  logEvent = TraceEvent.FINALLY
  
  override protected def start(): Unit = {
    trace("START", s"$node", logEvent)
    commonStart()
  }

  def startFinally(cause: Option[Throwable]) {
    trace("STFINALLY", s"$node $cause", logEvent)

    // If there's anyone reading from the errors port, send them the exception
    for (output <- node.outputs) {
      if (output == "errors") {
        if (cause.isDefined) {
          monitor ! GOutput(node, "errors", new ExceptionMessage(cause.get))
        }
        monitor ! GClose(node, "errors")
      }
    }

    for (child <- node.children) {
      monitor ! GStart(child)
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Finally]"
  }
}
