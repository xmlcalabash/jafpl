package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{Buffer, Node}
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput}
import com.jafpl.steps.DataConsumer

import scala.collection.mutable.ListBuffer

private[runtime] class BufferActor(private val monitor: ActorRef,
                                   private val runtime: GraphRuntime,
                                   private val node: Buffer)
  extends NodeActor(monitor, runtime, node) with DataConsumer {

  private var hasBeenReset = false
  private var buffer = ListBuffer.empty[Message]

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    runtime.runtime.deliver(from.id, fromPort, item, this, port)
  }

  override def id: String = node.id
  override def receive(port: String, item: Message): Unit = {
    buffer += item
    monitor ! GOutput(node, "result", item)
  }

  override protected def reset(): Unit = {
    readyToRun = true
    hasBeenReset = true
    openInputs.clear()
  }

  override protected def run(): Unit = {
    trace(s"RBUFR $node", "StepIO")
    if (hasBeenReset) {
      for (item <- buffer) {
        monitor ! GOutput(node, "result", item)
      }
    }
    for (output <- node.outputs) {
      monitor ! GClose(node, output)
    }
    monitor ! GFinished(node)
  }
}
