package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.LoopForStart
import com.jafpl.messages.{ItemMessage, Metadata}
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput, GRestartLoop}

import scala.collection.mutable

private[runtime] class LoopForActor(private val monitor: ActorRef,
                                    override protected val runtime: GraphRuntime,
                                    override protected val node: LoopForStart)
  extends LoopActor(monitor, runtime, node)  {

  private var current = node.countFrom
  logEvent = TraceEvent.LOOPFOR
  node.iterationPosition = 0L
  node.iterationSize = 0L

  override protected def reset(): Unit = {
    node.iterationPosition = 0L
    node.iterationSize = 0L
    super.reset()
  }

  override protected[runtime] def finished(): Unit = {
    current += node.countBy

    val pass = if (node.countBy > 0) {
      current <= node.countTo
    } else {
      current >= node.countTo
    }

    if (pass) {
      trace("ITERFIN", s"$node children:$childrenHaveFinished", logEvent)
      if (childrenHaveFinished) {
        monitor ! GRestartLoop(node)
      }
    } else {
      trace("LOOPFIN", s"$node", logEvent)
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

      if (childrenHaveFinished) {
        node.state = NodeState.FINISHED
        monitor ! GFinished(node)
        for (inj <- node.stepInjectables) {
          inj.afterRun()
        }
      }
    }
  }

  override protected def run(): Unit = {
    trace("RUN", s"$node running:$running ready:$started", logEvent)

    running = true
    node.state = NodeState.STARTED

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

    if (initiallyTrue) {
      node.iterationPosition += 1
      monitor ! GOutput(node, "current", new ItemMessage(current, Metadata.NUMBER))
      monitor ! GClose(node, "current")
      super.run()
    } else {
      for (port <- openOutputs) {
        trace("XYZ", s"$node 5 $port", logEvent)
        monitor ! GClose(node, port)
      }
      openOutputs.clear()

      finishIfReady()
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [LoopFor]"
  }
}
