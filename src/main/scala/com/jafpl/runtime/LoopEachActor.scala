package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{LoopEachStart, Node}
import com.jafpl.messages.{ItemMessage, Message, Metadata}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput, GReset, GStart}
import com.jafpl.steps.DataConsumer

import scala.collection.mutable.ListBuffer

private[runtime] class LoopEachActor(private val monitor: ActorRef,
                                     private val runtime: GraphRuntime,
                                     private val node: LoopEachStart)
  extends StartActor(monitor, runtime, node) with DataConsumer {

  private val queue = ListBuffer.empty[ItemMessage]
  private var running = false
  private var sourceClosed = false

  override protected def start(): Unit = {
    commonStart()
    runIfReady()
  }

  override protected def reset(): Unit = {
    super.reset()
    running = false
    readyToRun = true
    runIfReady()
  }

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    receive(port, item)
  }

  override def id: String = node.id
  override def receive(port: String, item: Message): Unit = {
    item match {
      case message: ItemMessage =>
        queue += message
      case _ =>
        monitor ! GException(None,
          JafplException.unexpectedMessage(item.toString, port, node.location))
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
        val edge = node.outputEdge("current")
        monitor ! GOutput(node, edge.fromPort, item)
        monitor ! GClose(node, edge.fromPort)

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
      commonFinished()
    } else {
      trace(s"RESET ForEach: $node $sourceClosed, ${queue.isEmpty}", "ForEach")
      monitor ! GReset(node)
    }
  }
}
