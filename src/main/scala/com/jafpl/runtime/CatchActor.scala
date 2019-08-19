package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.ContainerStart
import com.jafpl.messages.{ExceptionMessage, ItemMessage, Metadata}
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput, GStart}

private[runtime] class CatchActor(private val monitor: ActorRef,
                                  override protected val runtime: GraphRuntime,
                                  override protected val node: ContainerStart) extends StartActor(monitor, runtime, node)  {
  logEvent = TraceEvent.CATCH
  private var cause: Throwable = _

  override protected def configureOpenPorts(): Unit = {
    super.configureOpenPorts()
    openOutputs -= "error" // this one doesn't count
  }

  protected[runtime] def start(cause: Throwable): Unit = {
    this.cause = cause
    super.start()
  }

  override protected[runtime] def run(): Unit = {
    for (output <- node.outputs) {
      if (output == "error") {
        monitor ! GOutput(node, "error", new ExceptionMessage(cause))
        monitor ! GClose(node, "error")
      }
    }
    super.run()
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Catch]"
  }
}
