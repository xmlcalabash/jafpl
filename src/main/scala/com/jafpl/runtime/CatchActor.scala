package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.ContainerStart
import com.jafpl.messages.{ExceptionMessage, ItemMessage, Metadata}
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput, GStart}

private[runtime] class CatchActor(private val monitor: ActorRef,
                                  override protected val runtime: GraphRuntime,
                                  override protected val node: ContainerStart) extends StartActor(monitor, runtime, node)  {
  logEvent = TraceEvent.CATCH

  protected[runtime] def start(cause: Throwable): Unit = {
    trace("START", s"$node $cause", logEvent)

    commonStart()

    // If there's anyone reading from the errors port, send them the exception
    for (output <- node.outputs) {
      if (output == "error") {
        monitor ! GOutput(node, "error", new ExceptionMessage(cause))
        monitor ! GClose(node, "error")
      }
    }

    for (child <- node.children) {
      monitor ! GStart(child)
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Catch]"
  }
}
