package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{LoopForStart, Node}
import com.jafpl.messages.{ItemMessage, Message, Metadata}
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput, GRestartLoop, GStart}

import scala.collection.mutable

private[runtime] class LoopForActor(private val monitor: ActorRef,
                                    override protected val runtime: GraphRuntime,
                                    override protected val node: LoopForStart)
  extends StartActor(monitor, runtime, node)  {

  private var current = node.countFrom
  var running = false
  var looped = false
  val bindings = mutable.HashMap.empty[String, Any]

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
    super.reset() // yes, reset

    trace("RSTRTLOOP", s"$node", TraceEvent.METHODS)
    running = false
    readyToRun = true
    looped = false
    runIfReady()
  }

  override protected def input(from: Node, fromPort: String, port: String, msg: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", TraceEvent.METHODS)

    throw JafplException.noInputOnLoop(port, node.location)
  }

  protected[runtime] def loop(item: ItemMessage): Unit = {
    trace("LOOP", s"$node", TraceEvent.METHODS)
    looped = true
  }

  override protected def close(port: String): Unit = {
    trace("CLOSE", s"$node", TraceEvent.METHODS)
    throw JafplException.internalError("No port closures are expected on a for-loop", node.location)
  }

  private def runIfReady(): Unit = {
    trace("RUNIFREADY", s"$node running:$running ready:$readyToRun", TraceEvent.METHODS)

    if (!running && readyToRun) {
      running = true

      val initiallyTrue = if (node.countBy > 0) {
        current <= node.countTo
      } else {
        current >= node.countTo
      }

      trace("INITFLOP", s"Initially: $initiallyTrue: $current", TraceEvent.METHODS)

      if (initiallyTrue) {
        for (port <- node.outputs) {
          if (port == "current") {
            monitor ! GOutput(node, port, new ItemMessage(current, Metadata.NUMBER))
            monitor ! GClose(node, port)
          }
        }
        for (child <- node.children) {
          monitor ! GStart(child)
        }
      } else {
        finished()
      }
    }
  }

  override protected[runtime] def finished(): Unit = {
    current += node.countBy

    val pass = if (node.countBy > 0) {
      current <= node.countTo
    } else {
      current >= node.countTo
    }

    trace("FINISHED", s"$node $pass $current", TraceEvent.METHODS)

    if (pass) {
      monitor ! GRestartLoop(node)
    } else {
      checkCardinalities("current")

      // now close the outputs
      for (output <- node.outputs) {
        if (!node.inputs.contains(output)) {
          monitor ! GClose(node, output)
        }
      }
      monitor ! GFinished(node)
      commonFinished()
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [LoopFor]"
  }
}
