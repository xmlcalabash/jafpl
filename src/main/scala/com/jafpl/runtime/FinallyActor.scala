package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.ContainerStart
import com.jafpl.messages.ExceptionMessage
import com.jafpl.runtime.GraphMonitor.{GClose, GOutput, GStart}

private[runtime] class FinallyActor(private val monitor: ActorRef,
                                    private val runtime: GraphRuntime,
                                    private val node: ContainerStart) extends StartActor(monitor, runtime, node)  {
  override protected def start(): Unit = {
    commonStart()
  }

  def startFinally(cause: Option[Throwable]) {
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
      trace(s"START ... $child (for $node)", "Run")
      monitor ! GStart(child)
    }
  }
}
