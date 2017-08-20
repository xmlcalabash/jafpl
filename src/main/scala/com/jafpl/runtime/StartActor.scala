package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.ContainerStart
import com.jafpl.runtime.GraphMonitor.{GAbort, GFinished, GOutput, GReset, GStart, GStop, GStopped}

private[runtime] class StartActor(private val monitor: ActorRef,
                                  private val runtime: GraphRuntime,
                                  private val node: ContainerStart) extends NodeActor(monitor, runtime, node)  {
  override protected def start(): Unit = {
    readyToRun = true

    for (child <- node.children) {
      trace(s"START ... $child (for $node)", "Run")
      monitor ! GStart(child)
    }
  }

  override protected def abort(): Unit = {
    for (child <- node.children) {
      trace(s"ABORT ... $child (for $node)", "Run")
      monitor ! GAbort(child)
    }
    monitor ! GFinished(node)
  }

  override protected def stop(): Unit = {
    for (child <- node.children) {
      trace(s"STOPC ... $child (for $node)", "Stopping")
      monitor ! GStop(child)
    }
    monitor ! GStopped(node)
  }

  override protected def reset(): Unit = {
    readyToRun = false

    monitor ! GReset(node.end)
    for (child <- node.children) {
      trace(s"RESET ...$child (for $node)", "Run")
      monitor ! GReset(child)
    }
  }

  override protected def input(port: String, item: Any): Unit = {
    val edge = node.outputEdge(port)
    trace(s"Start actor $node sends to ${edge.toPort}: $item", "StepIO")
    monitor ! GOutput(node, edge.toPort, item)
  }

  protected[runtime] def finished(): Unit = {
    monitor ! GFinished(node)
  }
}
