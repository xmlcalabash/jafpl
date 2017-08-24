package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.ForEachStart
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput, GReset, GStart}

import scala.collection.mutable.ListBuffer

private[runtime] class ForEachActor(private val monitor: ActorRef,
                                    private val runtime: GraphRuntime,
                                    private val node: ForEachStart)
  extends StartActor(monitor, runtime, node)  {

  val queue = ListBuffer.empty[ItemMessage]
  var running = false
  var sourceClosed = false

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
      case _ => throw new PipelineException("badmessage", "Unexpected message $item on input")
    }
    runIfReady()
  }

  override protected def close(port: String): Unit = {
    sourceClosed = true
    runIfReady()
  }

  private def runIfReady(): Unit = {
    if (!running && readyToRun && queue.nonEmpty) {
      running = true

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
