package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{ContainerEnd, Node}
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.GFinishedViewport

import scala.collection.mutable.ListBuffer

private[runtime] class ViewportEndActor(private val monitor: ActorRef,
                                        override protected val runtime: GraphRuntime,
                                        override protected val node: ContainerEnd) extends EndActor(monitor, runtime, node)  {
  private val buffer = ListBuffer.empty[ItemMessage]

  override protected def reset(): Unit = {
    trace("RESET", "$node", TraceEvent.METHODS)
    super.reset()
    buffer.clear()
  }

  override protected def input(from: Node, fromPort: String, port: String, msg: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", TraceEvent.METHODS)
    msg match {
      case item: ItemMessage =>
        buffer += item
      case _ => Unit
    }
  }

  override protected def close(port: String): Unit = {
    trace("CLOSE", s"$node $port", TraceEvent.METHODS)
    openInputs -= port
    checkFinished()
    // Don't actually close the port...we're not done yet
  }

  override protected[runtime] def checkFinished(): Unit = {
    trace("CHKFINISH", s"${node.start.get}/end ready:$readyToRun inputs:${openInputs.isEmpty} children:${unfinishedChildren.isEmpty}", TraceEvent.METHODS)
    for (child <- unfinishedChildren) {
      trace(s"...UNFINIH", s"$child", TraceEvent.METHODS)
    }
    if (readyToRun) {
      if (openInputs.isEmpty && unfinishedChildren.isEmpty) {
        monitor ! GFinishedViewport(node, buffer.toList)
      }
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [ViewportEnd]"
  }
}
