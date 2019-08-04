package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{ContainerEnd, LoopForStart, Node}
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GException, GLoop, GOutput}

import scala.collection.mutable.ListBuffer

private[runtime] class LoopForEndActor(private val monitor: ActorRef,
                                       override protected val runtime: GraphRuntime,
                                       override protected val node: ContainerEnd) extends LoopEndActor(monitor, runtime, node) {
  logEvent = TraceEvent.LOOPFOREND

  override protected def reset(): Unit = {
    super.reset()

    // We got reset, so we're going around again.
    // That means the output we buffered on this loop is good.
    val loopForStart = node.start.get.asInstanceOf[LoopForStart]
    for (port <- loopForStart.buffer.keySet) {
      for (item <- loopForStart.buffer(port)) {
        node.start.get.outputCardinalities.put(port, node.start.get.outputCardinalities.getOrElse(port, 0L) + 1)
        monitor ! GOutput(node.start.get, port, item)
      }
    }
    loopForStart.buffer.clear()
  }

  override protected def input(from: Node, fromPort: String, port: String, msg: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", logEvent)

    if (port == "test") {
      // A loop sends it's output back to the start.
      msg match {
        case message: ItemMessage =>
          finished = false
          monitor ! GLoop(node.start.get, message)
        case _ =>
          monitor ! GException(None,
            JafplException.unexpectedMessage(msg.toString, port, node.location))
      }
    } else {
      val loopForStart = node.start.get.asInstanceOf[LoopForStart]
      if (!loopForStart.buffer.contains(port)) {
        loopForStart.buffer.put(port, ListBuffer.empty[Message])
      }
      loopForStart.buffer(port) += msg
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [LoopForEnd]"
  }
}
