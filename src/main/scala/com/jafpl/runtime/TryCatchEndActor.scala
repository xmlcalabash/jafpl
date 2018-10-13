package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{CatchStart, ContainerEnd, FinallyStart, Node, TryStart}
import com.jafpl.runtime.GraphMonitor.{GClose, GFinally, GFinished, GOutput}

private[runtime] class TryCatchEndActor(private val monitor: ActorRef,
                                        private val runtime: GraphRuntime,
                                        private val node: ContainerEnd) extends ConditionalEndActor(monitor, runtime, node)  {
  private var toldStart = false
  private var branchFinished = false
  private var ranFinally = false
  private var finblock = Option.empty[FinallyStart]

  override protected def reset(): Unit = {
    super.reset()

    for (child <- unfinishedChildren) {
      child match {
        case fin: FinallyStart =>
          finblock = Some(fin)
        case _ => Unit
      }
    }

    toldStart = false
    branchFinished = false
    ranFinally = finblock.isEmpty
  }

  override protected[runtime] def finished(otherNode: Node): Unit = {
    trace(s"CHILDFIN $otherNode", "StepFinished")

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
    trace(s"FINIFRDY ${node.start.get}/end ready:$readyToRun inputs:${openInputs.isEmpty} branch:$branchFinished fin:$ranFinally", "StepFinished")

    if (!toldStart && branchFinished) {
      if (ranFinally) {
        // If one branch has finished, and we've run the finally block, tell start we're done
        toldStart = true
        monitor ! GFinished(node)
      } else {
        ranFinally = true
        monitor ! GFinally(node.start.get)
      }
    }

    if (readyToRun) {
      if (openInputs.isEmpty) {
        readyToRun = false // don't run again if some joiner closes
        for (port <- buffer.keySet) {
          for (item <- buffer(port)) {
            node.outputCardinalities.put(port, node.outputCardinalities.getOrElse(port, 0L) + 1)
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
