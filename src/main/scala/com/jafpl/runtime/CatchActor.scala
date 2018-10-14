package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.ContainerStart
import com.jafpl.messages.{ExceptionMessage, ItemMessage, Metadata}
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput, GStart}

private[runtime] class CatchActor(private val monitor: ActorRef,
                                  override protected val runtime: GraphRuntime,
                                  override protected val node: ContainerStart) extends StartActor(monitor, runtime, node)  {
  protected[runtime] def start(cause: Throwable): Unit = {
    trace("START", s"$node $cause", TraceEvent.METHODS)

    commonStart()

    // If there's anyone reading from the errors port, send them the exception
    for (output <- node.outputs) {
      if (output == "errors") {
        monitor ! GOutput(node, "errors", new ExceptionMessage(cause))
        monitor ! GClose(node, "errors")
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
