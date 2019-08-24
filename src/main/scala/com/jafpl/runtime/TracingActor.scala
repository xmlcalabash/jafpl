package com.jafpl.runtime

import akka.actor.{Actor, ActorLogging}
import com.jafpl.graph.NodeState.NodeState
import com.jafpl.graph.{ContainerStart, Node, NodeState}
import com.jafpl.runtime.TraceEvent.TraceEvent

import scala.collection.mutable.ListBuffer

object TraceEvent extends Enumeration {
  type TraceEvent = Value
  val BUFFER, CATCH, CHOOSE, FINALLY, MONITOR, INPUT, JOINER,
  LOOPEACH, LOOPEACHEND, LOOPFOR, LOOPFOREND, LOOPUNTIL, LOOPUNTILEND,
  LOOPWHILE, LOOPWHILEEND, LOOPEND, END, NODE, OUTPUT, PIPELINE, SINK, SPLITTER,
  START, TRY, TRYEND, VARIABLE, VIEWPORT, VIEWPORTEND, WHEN, EMPTY,
  GMESSAGES, NMESSAGES, STEPIO, TRACES, CARDINALITY, RUNSTEP, BINDINGS, WATCHDOG,
  DEBUG, MESSAGE, STATECHANGE = Value
}

abstract class TracingActor(protected val runtime: GraphRuntime) extends Actor with ActorLogging {

  protected def fmtSender: String = {
    var str = sender().toString
    var pos = str.indexOf("/user/")
    str = str.substring(pos+6)
    pos = str.indexOf("#")
    str = str.substring(0, pos)
    str
  }

  protected def stateChange(node: Node, state: NodeState): Unit = {
    if (node.state == NodeState.STOPPED) {
      trace("CHANGEST!", s"$node: ${node.state} ignores $state", TraceEvent.STATECHANGE)
    } else {
      if (node.state == state) {
        trace("CHANGEST=", s"$node: ${node.state} already $state", TraceEvent.STATECHANGE)
      } else {
        trace("CHANGEST", s"$node: ${node.state} → $state", TraceEvent.STATECHANGE)
        node.state = state
      }
    }
  }

  protected def nodeState(node: Node): String = {
    val lb = ListBuffer.empty[String]
    node match {
      case start: ContainerStart =>
        for (cnode <- start.children) {
          lb += s"$cnode: ${cnode.state}"
        }
      case _ =>
        lb += s"${node.state}"
    }
    s"$node: ${lb.mkString(", ")}"
  }

  protected def trace(code: String, details: String, event: TraceEvent): Unit = {
    trace("info", code, details, event)
  }

  private def trace(level: String, code: String, details: String, event: TraceEvent): Unit = {
    val message = traceMessage(code, details)

    // We don't use the traceEventManager.trace() call because we want to use the Akka logger
    if (runtime.traceEventManager.traceEnabled(event.toString.toLowerCase())) {
      level match {
        case "info" => log.info(message)
        case "debug" => log.debug(message)
        case _ => log.warning(message)
      }
    }
  }

  protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details
  }
}
