package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{LoopEachStart, Node}
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput, GRestartLoop, GStart}
import com.jafpl.steps.DataConsumer

import scala.collection.mutable.ListBuffer

private[runtime] class LoopEachActor(private val monitor: ActorRef,
                                     override protected val runtime: GraphRuntime,
                                     override protected val node: LoopEachStart)
  extends StartActor(monitor, runtime, node) with DataConsumer {

  private val queue = ListBuffer.empty[ItemMessage]
  private var running = false
  private var sourceClosed = false
  logEvent = TraceEvent.LOOPEACH

  override protected def start(): Unit = {
    trace("START", s"$node", logEvent)
    running = false
    commonStart()
    runIfReady()
  }

  override protected def reset(): Unit = {
    super.reset()
    trace("RESTART", s"$node", logEvent)
    running = false
    readyToRun = true
    sourceClosed = false
    runIfReady()
  }

  protected[runtime] def restartLoop(): Unit = {
    trace("RSTRTLOOP", s"$node", logEvent)
    super.reset() // yes, reset
    running = false
    readyToRun = true
    runIfReady()
  }

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", logEvent)
    receive(port, item)
  }

  override def receive(port: String, item: Message): Unit = {
    trace("RECEIVE", s"$node $port", logEvent)
    item match {
      case message: ItemMessage =>
        queue += message
      case _ =>
        monitor ! GException(Some(node), JafplException.unexpectedMessage(item.toString, port, node.location))
    }
    runIfReady()
  }

  override protected def close(port: String): Unit = {
    trace("CLOSE", s"$node $port", logEvent)
    sourceClosed = true
    runIfReady()
  }

  private def runIfReady(): Unit = {
    trace("RUNIFREADY", s"$node ready:$readyToRun closed:$sourceClosed", logEvent)
    if (!running && readyToRun && sourceClosed) {
      running = true

      if (queue.nonEmpty) {
        val item = queue.head
        queue -= item
        val edge = node.outputEdge("current")
        monitor ! GOutput(node, edge.fromPort, item)
        monitor ! GClose(node, edge.fromPort)
        for (child <- node.children) {
          monitor ! GStart(child)
        }
      } else {
        finished()
      }
    }
  }

  override protected[runtime] def finished(): Unit = {
    trace("FINISHED", s"$node closed:$sourceClosed queue:${queue.isEmpty}", logEvent)

    if (sourceClosed && queue.isEmpty) {
      checkCardinalities("current")

      // now close the outputs
      for (output <- node.outputs) {
        if (!node.inputs.contains(output)) {
          // Don't close 'current'; it must have been closed to get here and re-closing
          // it propagates the close event to the steps and they shouldn't see any more
          // events!
          if (output != "current") {
            monitor ! GClose(node, output)
          }
        }
      }

      monitor ! GFinished(node)
      commonFinished()
    } else {
      monitor ! GRestartLoop(node)
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [LoopEach]"
  }
}
