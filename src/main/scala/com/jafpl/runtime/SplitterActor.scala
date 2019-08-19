package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{Edge, Node, Splitter}
import com.jafpl.messages.{BindingMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput}

import scala.collection.mutable.ListBuffer

private[runtime] class SplitterActor(private val monitor: ActorRef,
                                     override protected val runtime: GraphRuntime,
                                     override protected val node: Splitter)
  extends NodeActor(monitor, runtime, node) {

  var edges: Option[ListBuffer[Edge]] = None
  logEvent = TraceEvent.SPLITTER
  
  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", logEvent)
    // Cache the edges for a small amount of efficiency
    if (edges.isEmpty) {
      val outbound = ListBuffer.empty[Edge]
      for (port <- node.outputs) {
        outbound += node.outputEdge(port)
      }
      edges = Some(outbound)
    }

    // Not "else" because we may have just populated edges!
    if (edges.isDefined) {
      for (edge <- edges.get) {
        //println(s"Splitter sends $item on ${edge.fromPort} to ${edge.toPort}")
        trace("SPLITTER", s"$node sends $item to $node on ${edge.fromPort}", TraceEvent.STEPIO)
        monitor ! GOutput(node, edge.fromPort, item)
      }
    }

    if (port == "#bindings") {
      item match {
        case _: BindingMessage => Unit
        case _ =>
          monitor ! GException(None,
            JafplException.unexpectedMessage(item.toString, port, node.location))
      }
    }
  }

  override protected def run(): Unit = {
    trace("RUN", s"$node", logEvent)
    for (edge <- edges.get) {
      monitor ! GClose(node, edge.fromPort)
    }
    node.state = NodeState.FINISHED
    monitor ! GFinished(node)
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Splitter]"
  }
}
