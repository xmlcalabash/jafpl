package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.Node
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.GException

private[runtime] class OutputActor(private val monitor: ActorRef,
                                   private val runtime: GraphRuntime,
                                   private val node: Node,
                                   private val consumer: OutputProxy)
  extends NodeActor(monitor, runtime, node, consumer) {
  private var closed = false

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    item match {
      case item: ItemMessage =>
        if (consumer.provider.isDefined) {
          trace(s"DELIVER→ $node.$port → ${consumer.provider.get}.$port", "StepIO")
          consumer.provider.get.receive(port, item)
        } else {
          trace(s"↴DELIVER $node.$port (no consumer)", "StepIO")
        }
      case _ =>
        monitor ! GException(None,
          PipelineException.BADMESSAGE(s"Unexpected message $item on $port", node.location))
    }
  }

  override protected def close(port: String): Unit = {
    closed = true
    runIfReady()
  }

  override protected def start(): Unit = {
    readyToRun = true
    runIfReady()
  }

  private def runIfReady(): Unit = {
    trace(s"RUNIFRDY $node ready:$readyToRun closed:$closed", "StepExec")

    if (readyToRun && closed) {
      run()
    }
  }
}
