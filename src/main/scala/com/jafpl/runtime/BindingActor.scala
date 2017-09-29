package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.Binding
import com.jafpl.messages.{BindingMessage, ItemMessage, Metadata}
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput}

private[runtime] class BindingActor(private val monitor: ActorRef,
                                    private val runtime: GraphRuntime,
                                    private val binding: Binding,
                                    private val provider: BindingProxy)
  extends NodeActor(monitor, runtime, binding)  {

  override protected def start(): Unit = {
    readyToRun = true
    runIfReady()
  }

  private def runIfReady(): Unit = {
    trace(s"RUNIFRDY $binding ready:$readyToRun closed:${provider.closed})", "StepExec")

    if (readyToRun && provider.closed) {
      run()
    }
  }

  override protected def run(): Unit = {
    if (provider.value.isDefined) {
      val msg = new BindingMessage(binding.name, new ItemMessage(provider.value.get, Metadata.ANY))
      monitor ! GOutput(binding, "result", msg)
    }
    monitor ! GClose(binding, "result")
    monitor ! GFinished(binding)
  }
}
