package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.GraphException
import com.jafpl.graph.Binding
import com.jafpl.messages.BindingMessage
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput}

import scala.collection.mutable

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
    trace(s"RNIFR $binding $readyToRun ${provider.closed}", "StepExec")

    if (readyToRun && provider.closed) {
      run()
    }
  }

  override protected def run(): Unit = {
    val msg = new BindingMessage(binding.name, provider.value.get)
    monitor ! GOutput(binding, "result", msg)
    monitor ! GClose(binding, "result")
    monitor ! GFinished(binding)
  }
}
