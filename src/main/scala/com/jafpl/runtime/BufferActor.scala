package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.Buffer
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput}

import scala.collection.mutable.ListBuffer

private[runtime] class BufferActor(private val monitor: ActorRef,
                                   private val runtime: GraphRuntime,
                                   private val node: Buffer)
  extends NodeActor(monitor, runtime, node)  {

  var hasBeenReset = false
  var buffer = ListBuffer.empty[Message]

  override protected def input(port: String, item: Message): Unit = {
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
