package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{LoopUntilStart, Node}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput, GReset, GRestartLoop, GStart}
import com.jafpl.steps.DataConsumer

import scala.collection.mutable

private[runtime] class LoopUntilActor(private val monitor: ActorRef,
                                      override protected val runtime: GraphRuntime,
                                      override protected val node: LoopUntilStart)
  extends StartActor(monitor, runtime, node) with DataConsumer {

  var currentItem = Option.empty[ItemMessage]
  var nextItem = Option.empty[ItemMessage]
  var running = false
  var looped = false
  val bindings = mutable.HashMap.empty[String, Message]

  override protected def start(): Unit = {
    trace("START", s"$node", TraceEvent.METHODS)
    commonStart()
    runIfReady()
  }

  override protected def reset(): Unit = {
    trace("RESET", s"$node", TraceEvent.METHODS)
    super.reset()
    running = false
    readyToRun = true
    looped = false
    runIfReady()
  }

  protected[runtime] def restartLoop(): Unit = {
    trace("RSTRTLOOP", s"$node", TraceEvent.METHODS)
    super.reset() // yes, reset
    running = false
    readyToRun = true
    looped = false
    runIfReady()
  }

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", TraceEvent.METHODS)
    receive(port, item)
  }

  override def receive(port: String, item: Message): Unit = {
    trace("RECEIVE", s"$node $port", TraceEvent.METHODS)
    if (port == "source") {
      item match {
        case message: ItemMessage =>
          if (currentItem.isDefined) {
            monitor ! GException(None,
              JafplException.unexpectedSequence(node.toString, port, node.location))
            return
          }
          currentItem = Some(message)
        case _ =>
          monitor ! GException(None,
            JafplException.unexpectedMessage(item.toString, port, node.location))
          return
      }
    } else if (port == "#bindings") {
      item match {
        case msg: BindingMessage =>
          bindings.put(msg.name, msg)
        case _ =>
          monitor ! GException(None,
            JafplException.unexpectedMessage(item.toString, port, node.location))
          return
      }
    }
    runIfReady()
  }

  protected[runtime] def loop(item: ItemMessage): Unit = {
    trace("LOOP", s"$node", TraceEvent.METHODS)
    nextItem = Some(item)
    looped = true
  }

  override protected def close(port: String): Unit = {
    trace("CLOSE", s"$node $port", TraceEvent.METHODS)
    runIfReady()
  }

  private def runIfReady(): Unit = {
    trace("RUNIFREADY", s"$node ready:$readyToRun def:${currentItem.isDefined}", TraceEvent.METHODS)
    if (!running && readyToRun && currentItem.isDefined) {
      running = true

      val edge = node.outputEdge("current")
      monitor ! GOutput(node, edge.fromPort, currentItem.get)
      monitor ! GClose(node, edge.fromPort)

      for (child <- node.children) {
        monitor ! GStart(child)
      }
    }
  }

  override protected[runtime] def finished(): Unit = {
    val finished = node.comparator.areTheSame(currentItem.get.item, nextItem.get.item)

    trace("FINISHED", s"$node ${currentItem.get}: ${nextItem.get}: $finished", TraceEvent.METHODS)

    if (finished) {
      checkCardinalities("current")
      monitor ! GOutput(node, "result", nextItem.get)
      // now close the outputs
      for (output <- node.outputs) {
        if (!node.inputs.contains(output)) {
          monitor ! GClose(node, output)
        }
      }
      monitor ! GFinished(node)
      commonFinished()
    } else {
      currentItem = nextItem
      nextItem = None
      monitor ! GRestartLoop(node)
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [LoopUntil]"
  }
}
