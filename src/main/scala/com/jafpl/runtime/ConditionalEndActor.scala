package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{ContainerEnd, Node}
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class ConditionalEndActor(private val monitor: ActorRef,
                                           override protected val runtime: GraphRuntime,
                                           override protected val node: ContainerEnd) extends EndActor(monitor, runtime, node)  {
  val buffer = mutable.HashMap.empty[String, ListBuffer[Message]]

  override protected def reset(): Unit = {
    trace("RESET", s"$node", TraceEvent.METHODS)
    super.reset()
    buffer.clear()
  }

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$from.$fromPort -> $port", TraceEvent.METHODS)

    // Buffer everything in case it all goes bang
    if (!buffer.contains(port)) {
      buffer.put(port, ListBuffer.empty[Message])
    }
    buffer(port) += item
  }

  override protected def close(port: String): Unit = {
    trace("CLOSE", s"$node", TraceEvent.METHODS)
    openInputs -= port
    checkFinished()
  }

  override protected[runtime] def checkFinished(): Unit = {
    trace("CHKFINISH", s"${node.start.get}/end ready:$readyToRun inputs:${openInputs.isEmpty} children:${unfinishedChildren.isEmpty}", TraceEvent.METHODS)
    for (child <- unfinishedChildren) {
      trace(s"...UNFINSH", s"$child", TraceEvent.METHODS)
    }
    if (readyToRun) {
      if (openInputs.isEmpty && unfinishedChildren.isEmpty) {
        for (port <- buffer.keySet) {
          for (item <- buffer(port)) {
            node.start.get.outputCardinalities.put(port, node.start.get.outputCardinalities.getOrElse(port, 0L) + 1)
            monitor ! GOutput(node.start.get, port, item)
          }
        }
        for (input <- node.inputs) {
          monitor ! GClose(node.start.get, input)
        }
        monitor ! GFinished(node)
      }
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [CondEnd]"
  }
}
