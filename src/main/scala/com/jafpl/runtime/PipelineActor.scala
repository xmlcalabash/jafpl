package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.ContainerStart
import com.jafpl.runtime.GraphMonitor.GClose

private[runtime] class PipelineActor(private val monitor: ActorRef,
                                     override protected val runtime: GraphRuntime,
                                     override protected val node: ContainerStart) extends StartActor(monitor, runtime, node) {

  override protected def close(port: String): Unit = {
    // Closing pipeline outputs isn't conditional (because they never loop)
    if (openOutputs.contains(port)) {
      openOutputs -= port
      trace("CLOSEO", s"$node.$port: $childState : $openOutputs", logEvent)
      monitor ! GClose(node, port)
      finishIfReady()
    } else {
      super.close(port)
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Pipeline]"
  }
}
