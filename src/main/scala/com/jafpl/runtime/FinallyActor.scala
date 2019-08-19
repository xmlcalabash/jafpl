package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.ContainerStart
import com.jafpl.messages.ExceptionMessage
import com.jafpl.runtime.GraphMonitor.{GClose, GOutput, GStart}

private[runtime] class FinallyActor(private val monitor: ActorRef,
                                    override protected val runtime: GraphRuntime,
                                    override protected val node: ContainerStart) extends StartActor(monitor, runtime, node)  {
  logEvent = TraceEvent.FINALLY
  private var cause = Option.empty[Throwable]

  override protected def configureOpenPorts(): Unit = {
    super.configureOpenPorts()
    openOutputs -= "error" // this one doesn't count
  }

  def startFinally(cause: Option[Throwable]) {
    this.cause = cause
    super.start()
  }

  override protected[runtime] def run(): Unit = {
    // If there's anyone reading from the errors port, send them the exception
    for (output <- node.outputs) {
      if (output == "error") {
        if (cause.isDefined) {
          monitor ! GOutput(node, "error", new ExceptionMessage(cause.get))
        }
        monitor ! GClose(node, "error")
      }
    }

    super.run()
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Finally]"
  }
}
