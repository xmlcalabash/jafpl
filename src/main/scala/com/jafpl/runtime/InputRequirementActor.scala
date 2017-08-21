package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.Node
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished}
import com.jafpl.steps.StepDataProvider

private[runtime] class InputRequirementActor(private val monitor: ActorRef,
                                             private val runtime: GraphRuntime,
                                             private val node: Node,
                                             private val consumer: InputProxy)
  extends NodeActor(monitor, runtime, node, consumer) {

  override protected def start(): Unit = {
    readyToRun = true
    runIfReady()
  }

  private def runIfReady(): Unit = {
    trace(s"RNIFR $node $readyToRun ${consumer.closed}", "StepExec")

    if (readyToRun && consumer.closed) {
      run()
    }
  }
}
