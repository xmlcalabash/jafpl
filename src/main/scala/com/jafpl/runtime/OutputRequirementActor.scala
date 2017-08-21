package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.Node
import com.jafpl.runtime.GraphMonitor.GFinished

private[runtime] class OutputRequirementActor(private val monitor: ActorRef,
                                              private val runtime: GraphRuntime,
                                              private val node: Node,
                                              private val consumer: OutputProxy)
  extends NodeActor(monitor, runtime, node, consumer) {
  private var closed = false

  override protected def input(port: String, item: Any): Unit = {
    if (consumer.provider.isDefined) {
      consumer.provider.get.send(item)
    }
  }

  override protected def close(port: String): Unit = {
    if (consumer.provider.isDefined) {
      consumer.provider.get.close()
    }
    closed = true
    runIfReady()
  }

  override protected def start(): Unit = {
    readyToRun = true
    runIfReady()
  }

  private def runIfReady(): Unit = {
    trace(s"RNIFR $node $readyToRun ${closed}", "StepExec")

    if (readyToRun && closed) {
      run()
    }
  }
}
