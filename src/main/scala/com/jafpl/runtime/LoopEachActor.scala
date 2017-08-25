package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.LoopEachStart
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput, GReset, GStart}
import com.jafpl.util.PipelineMessage

import scala.collection.mutable.ListBuffer

private[runtime] class LoopEachActor(private val monitor: ActorRef,
                                     private val runtime: GraphRuntime,
                                     private val node: LoopEachStart)
  extends StartActor(monitor, runtime, node)  {

  private val queue = ListBuffer.empty[ItemMessage]
  private var running = false
  private var sourceClosed = false

  override protected def start(): Unit = {
    readyToRun = true
    runIfReady()
  }

  override protected def reset(): Unit = {
    super.reset()
    running = false
    readyToRun = true
    runIfReady()
  }

  override protected def input(port: String, item: Message): Unit = {
    item match {
      case message: ItemMessage =>
        queue += message
      case _ =>
        monitor ! GException(None,
          new PipelineException("badmessage", "Unexpected message $item on input", node.location))
    }
    runIfReady()
  }

  override protected def close(port: String): Unit = {
    sourceClosed = true
    runIfReady()
  }

  private def runIfReady(): Unit = {
    if (!running && readyToRun && sourceClosed) {
      running = true

      if (queue.nonEmpty) {
        val item = queue.head
        queue -= item
        val edge = node.outputEdge("source")
        monitor ! GOutput(node, edge.toPort, item)
        monitor ! GClose(node, edge.toPort)

        trace(s"START ForEach: $node", "ForEach")

        for (child <- node.children) {
          trace(s"START ...$child (for $node)", "ForEach")
          monitor ! GStart(child)
        }
      } else {
        finished()
      }
    }
  }

  override protected[runtime] def finished(): Unit = {
    if (sourceClosed && queue.isEmpty) {
      trace(s"FINISH ForEach: $node $sourceClosed, ${queue.isEmpty}", "ForEach")
      // now close the outputs
      for (output <- node.outputs) {
        if (!node.inputs.contains(output)) {
          monitor ! GClose(node, output)
        }
      }
      monitor ! GFinished(node)
    } else {
      trace(s"RESET ForEach: $node $sourceClosed, ${queue.isEmpty}", "ForEach")
      monitor ! GReset(node)
    }
  }
}
