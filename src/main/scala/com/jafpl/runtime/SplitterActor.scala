package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.{GraphException, PipelineException}
import com.jafpl.graph.{Edge, Splitter}
import com.jafpl.messages.{BindingMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GException, GOutput}

import scala.collection.mutable.ListBuffer

private[runtime] class SplitterActor(private val monitor: ActorRef,
                                     private val runtime: GraphRuntime,
                                     private val node: Splitter)
  extends NodeActor(monitor, runtime, node)  {

  var edges: Option[ListBuffer[Edge]] = None

  override protected def input(port: String, item: Message): Unit = {
    // Cache the edges for a small amount of efficiency
    if (edges.isEmpty) {
      val outbound = ListBuffer.empty[Edge]
      for (port <- node.outputs) {
        outbound += node.outputEdge(port)
      }
      edges = Some(outbound)
    }

    if (edges.isDefined) {
      for (edge <- edges.get) {
        trace("SPLTR Sends " + item + " to " + node + " on " + edge.fromPort, "Splitter")
        monitor ! GOutput(node, edge.fromPort, item)
      }
    }

    if (port == "#bindings") {
      item match {
        case binding: BindingMessage =>
          openBindings -= binding.name
        case _ =>
          monitor ! GException(None,
            new PipelineException("badbinding", "Unexpected item on #bindings port", node.location))
      }
    }
  }
}
