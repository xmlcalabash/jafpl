package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{ContainerStart, Node}
import com.jafpl.messages.{BindingMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GAbort, GClose, GFinished, GOutput, GReset, GStart, GStop, GStopped}

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
    monitor ! GStop(node.containerEnd)
    monitor ! GStopped(node)
  }

  override protected def reset(): Unit = {
    readyToRun = false

    monitor ! GReset(node.containerEnd)
    for (child <- node.children) {
      trace(s"RESET ...$child (for $node)", "Run")
      monitor ! GReset(child)
    }
  }

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    // Bindings on compound steps are only ever for injectables
    if (port == "#bindings") {
      for (inj <- node.stepInjectables) {
        item match {
          case binding: BindingMessage =>
            inj.receiveBinding(binding)
          case _ =>
            throw new RuntimeException("FIXME: bang")
        }
      }
    } else {
      val edge = node.outputEdge(port)
      trace(s"Start actor $node sends to ${edge.toPort}: $item", "StepIO")
      monitor ! GOutput(node, edge.fromPort, item)
    }
  }

  protected[runtime] def finished(): Unit = {
    for (output <- node.outputs) {
      monitor ! GClose(node, output)
    }
    monitor ! GFinished(node)
    for (inj <- node.stepInjectables) {
      inj.afterRun()
    }
  }
}
