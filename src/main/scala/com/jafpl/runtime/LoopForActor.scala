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
  logEvent = TraceEvent.LOOPFOR
  node.iterationPosition = 0L
  node.iterationSize = 0L

  override protected def start(): Unit = {
    trace("START", s"$node", logEvent)
    commonStart()
    runIfReady()
  }

  override protected def reset(): Unit = {
    trace("RESET", s"$node", logEvent)
    super.reset()
    running = false
    readyToRun = true
    looped = false
    node.iterationSize = 0L
    node.iterationPosition = 0L
    runIfReady()
  }

  protected[runtime] def restartLoop(): Unit = {
    super.reset() // yes, reset

    trace("RSTRTLOOP", s"$node", logEvent)
    running = false
    readyToRun = true
    looped = false
    runIfReady()
  }

  override protected def input(from: Node, fromPort: String, port: String, msg: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", logEvent)

    throw JafplException.noInputOnLoop(port, node.location)
  }

  protected[runtime] def loop(item: ItemMessage): Unit = {
    trace("LOOP", s"$node", logEvent)
    looped = true
  }

  override protected def close(port: String): Unit = {
    trace("CLOSE", s"$node", logEvent)
    throw JafplException.internalError("No port closures are expected on a for-loop", node.location)
  }

  private def runIfReady(): Unit = {
    trace("RUNIFREADY", s"$node running:$running ready:$readyToRun", logEvent)

    if (!running && readyToRun) {
      running = true

      if (node.iterationSize == 0) {
        val diff = if (node.countFrom > node.countTo) {
          node.countFrom - node.countTo + 1
        } else {
          node.countTo - node.countFrom + 1
        }
        node.iterationSize = (diff + node.countBy.abs - 1) / node.countBy.abs
      }

      val initiallyTrue = if (node.countBy > 0) {
        current <= node.countTo
      } else {
        current >= node.countTo
      }

      trace("INITFLOP", s"Initially: $initiallyTrue: $current", logEvent)

      if (initiallyTrue) {
        node.iterationPosition += 1
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

    trace("FINISHED", s"$node $pass $current", logEvent)

    if (pass) {
      monitor ! GRestartLoop(node)
    } else {
      checkCardinalities("current")

      for (port <- node.buffer.keySet) {
        for (item <- node.buffer(port)) {
          node.outputCardinalities.put(port, node.outputCardinalities.getOrElse(port, 0L) + 1)
          monitor ! GOutput(node, port, item)
        }
      }
      node.buffer.clear()

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
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [LoopFor]"
  }
}
