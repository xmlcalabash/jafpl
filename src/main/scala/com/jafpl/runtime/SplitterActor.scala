package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{Edge, Node, Splitter}
import com.jafpl.messages.{BindingMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GException, GOutput}
import com.jafpl.steps.DataConsumer

import scala.collection.mutable.ListBuffer

private[runtime] class SplitterActor(private val monitor: ActorRef,
                                     override protected val runtime: GraphRuntime,
                                     override protected val node: Splitter)
  extends NodeActor(monitor, runtime, node) with DataConsumer {

  var edges: Option[ListBuffer[Edge]] = None

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", TraceEvent.METHODS)
    receive(port, item)
  }

  override def receive(port: String, item: Message): Unit = {
    trace("RECEIVE", s"$node $port", TraceEvent.METHODS)
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
        trace("SPLITTER", s"$node sends $item to $node on ${edge.fromPort}", TraceEvent.STEPIO)
        monitor ! GOutput(node, edge.fromPort, item)
      }
    }

    if (port == "#bindings") {
      item match {
        case binding: BindingMessage => Unit
        case _ =>
          monitor ! GException(None,
            JafplException.unexpectedMessage(item.toString, port, node.location))
      }
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Splitter]"
  }
}
