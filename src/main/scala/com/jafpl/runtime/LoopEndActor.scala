package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.ContainerEnd
import com.jafpl.runtime.GraphMonitor.GException

private[runtime] class LoopEndActor(private val monitor: ActorRef,
                                    override protected val runtime: GraphRuntime,
                                    override protected val node: ContainerEnd) extends EndActor(monitor, runtime, node)  {
  override protected def close(port: String): Unit = {
    trace("CLOSE", s"$node $port", TraceEvent.METHODS)
    try {
      // Are we closing an input or an output?
      val manifold = node.start.get.manifold
      if (manifold.isDefined) {
        if (manifold.get.inputSpec.ports.contains(port)) {
          val count = node.start.get.inputCardinalities.getOrElse(port, 0L)
          manifold.get.inputSpec.checkInputCardinality(port, count)
        } else if (manifold.get.outputSpec.ports.contains(port)) {
          val count = node.start.get.outputCardinalities.getOrElse(port, 0L)
          manifold.get.outputSpec.checkOutputCardinality(port, count)
        }
      }
    } catch {
      case ex: JafplException =>
        monitor ! GException(Some(node), ex)
    }

    openInputs -= port
    checkFinished()
    // Don't actually close the port...we're not done yet
  }
}
