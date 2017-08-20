package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.ContainerEnd
import com.jafpl.runtime.GraphMonitor.GFinishedViewport

import scala.collection.mutable.ListBuffer

private[runtime] class ViewportEndActor(private val monitor: ActorRef,
                                        private val runtime: GraphRuntime,
                                        private val node: ContainerEnd) extends EndActor(monitor, runtime, node)  {
  val buffer = ListBuffer.empty[Any]

  override protected def reset(): Unit = {
    super.reset()
    buffer.clear()
  }

  override protected def input(port: String, item: Any): Unit = {
    if (port == "result") {
      buffer += item
    }
  }

  override protected def close(port: String): Unit = {
    openInputs -= port
    checkFinished()
    // Don't actually close the port...we're not done yet
  }

  override protected[runtime] def checkFinished(): Unit = {
    trace(s"FNIFR VE $node (${node.start.getOrElse("!START")}) $readyToRun ${openInputs.isEmpty}: ${unfinishedChildren.isEmpty}", "StepFinished")
    for (child <- unfinishedChildren) {
      trace(s"!FNSH ...$child", "StepFinished")
    }
    if (readyToRun) {
      if (openInputs.isEmpty && unfinishedChildren.isEmpty) {
        trace(s"FVPRT $node", "StepFinished")
        monitor ! GFinishedViewport(node, buffer.toList)
      }
    }
  }
}
