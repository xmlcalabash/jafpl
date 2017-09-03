package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.ContainerStart
import com.jafpl.messages.{ExceptionMessage, ItemMessage, Metadata}
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput, GStart}

private[runtime] class CatchActor(private val monitor: ActorRef,
                                  private val runtime: GraphRuntime,
                                  private val node: ContainerStart) extends StartActor(monitor, runtime, node)  {
  protected[runtime] def start(cause: Throwable): Unit = {
    readyToRun = true

    // If there's anyone reading from the errors port, send them the exception
    for (output <- node.outputs) {
      if (output == "errors") {
        monitor ! GOutput(node, "errors", new ExceptionMessage(cause))
        monitor ! GClose(node, "errors")
      }
    }

    for (child <- node.children) {
      trace(s"START ... $child (for $node)", "Run")
      monitor ! GStart(child)
    }
  }
}
