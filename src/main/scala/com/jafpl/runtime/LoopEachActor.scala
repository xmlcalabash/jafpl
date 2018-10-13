package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{LoopEachStart, Node}
import com.jafpl.messages.{ItemMessage, Message, Metadata}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput, GReset, GRestartLoop, GStart}
import com.jafpl.steps.{DataConsumer, Manifold}

import scala.collection.mutable.ListBuffer

private[runtime] class LoopEachActor(private val monitor: ActorRef,
                                     private val runtime: GraphRuntime,
                                     private val node: LoopEachStart)
  extends StartActor(monitor, runtime, node) with DataConsumer {

  private val queue = ListBuffer.empty[ItemMessage]
  private var running = false
  private var sourceClosed = false

  override protected def start(): Unit = {
    trace(s"MSTART $node", "Methods")
    running = false
    commonStart()
    runIfReady()
  }

  override protected def reset(): Unit = {
    super.reset()
    trace(s"MRESET $node", "Methods")
    running = false
    readyToRun = true
    sourceClosed = false
    runIfReady()
  }

  protected[runtime] def restartLoop(): Unit = {
    trace(s"MRELOOP $node", "Methods")
    super.reset() // yes, reset
    running = false
    readyToRun = true
    runIfReady()
  }

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    receive(port, item)
  }

  override def id: String = node.id
  override def receive(port: String, item: Message): Unit = {
    trace(s"MRECV  $node", "Methods")
    item match {
      case message: ItemMessage =>
        queue += message
      case _ =>
        monitor ! GException(Some(node), JafplException.unexpectedMessage(item.toString, port, node.location))
    }
    runIfReady()
  }

  override protected def close(port: String): Unit = {
    trace(s"MCLOSE $node", "Methods")
    sourceClosed = true
    runIfReady()
  }

  private def runIfReady(): Unit = {
    trace(s"MRIFRD $node ${!running} $readyToRun $sourceClosed", "Methods")

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
    trace(s"MFINSH $node $sourceClosed ${queue.isEmpty}", "Methods")

    if (sourceClosed && queue.isEmpty) {
      trace(s"FINISH ForEach: $node $sourceClosed, ${queue.isEmpty}", "ForEach")

      checkCardinalities("current")

      // now close the outputs
      for (output <- node.outputs) {
        if (!node.inputs.contains(output)) {
          monitor ! GClose(node, output)
        }
      }

      trace(s"RSTCRD $node", "Cardinality")
      monitor ! GFinished(node)
      commonFinished()
    } else {
      trace(s"RESTART ForEach: $node $sourceClosed, ${queue.isEmpty}", "ForEach")
      monitor ! GRestartLoop(node)
    }
  }
}
