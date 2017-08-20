package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{CatchStart, ContainerEnd, Node, TryStart}
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput}

private[runtime] class TryCatchEndActor(private val monitor: ActorRef,
                                        private val runtime: GraphRuntime,
                                        private val node: ContainerEnd) extends ConditionalEndActor(monitor, runtime, node)  {
  private var toldStart = false
  private var branchFinished = false

  override protected def reset(): Unit = {
    super.reset()
    toldStart = false
    branchFinished = false
  }

  override protected[runtime] def finished(otherNode: Node): Unit = {
    trace(s"END FINISHED ${node.start} / $otherNode", "StepFinished")

    // Only one child of a try-catch will ever run, so if we get called, something succeeded
    // Well. Not if splitters or joiners get called.
    otherNode match {
      case other: TryStart =>
        branchFinished = true
      case other: CatchStart =>
        branchFinished = true
      case _ => Unit
    }

    if (branchFinished) {
      checkFinished()
    }
  }

  override protected[runtime] def checkFinished(): Unit = {
    trace(s"FNIFR $node (${node.start.getOrElse("!START")}) $readyToRun $branchFinished ${openInputs.isEmpty}", "StepFinished")

    if (!toldStart && branchFinished) {
      // As soon one branch finishes, tell the start that we're done
      toldStart = true
      trace(s"FINSH $node", "StepFinished")
      monitor ! GFinished(node)
    }

    if (readyToRun) {
      if (openInputs.isEmpty) {
        readyToRun = false // don't run again if some joiner closes
        for (port <- buffer.keySet) {
          for (item <- buffer(port)) {
            trace(s"Try/catch end actor ${node.start} sends to ${port}: $item", "StepIO")
            monitor ! GOutput(node.start.get, port, item)
          }
        }
        for (input <- node.inputs) {
          monitor ! GClose(node.start.get, input)
        }
      }
    }
  }
}
