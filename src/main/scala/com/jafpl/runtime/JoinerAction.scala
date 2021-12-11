package com.jafpl.runtime

import com.jafpl.graph.{JoinMode, Joiner}
import com.jafpl.messages.Message
import com.jafpl.util.TraceEventManager

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class JoinerAction(override val node: Joiner) extends AbstractAction(node) {
  private val ports = mutable.HashSet.empty[String]
  private val messages = ListBuffer.empty[Tuple2[String,Message]]

  override def receive(port: String, message: Message): Unit = {
    tracer.trace(s"RECV  $this for $port", TraceEventManager.RECEIVE)
    tracer.trace(s"MESSAGE $message", TraceEventManager.MESSAGES)
    messages += Tuple2(port, message)
    ports.add(port)
  }

  override def run(): Unit = {
    super.run()

    var mode = node.mode
    if (mode == JoinMode.PRIORITY && !ports.contains("source_1")) {
      // I've forgottene exactly what "priority" was for. When I reworked
      // this code to preserver order, I determined that if the mode was
      // "priority" and there were documents on the "source_1" port, that's
      // what was returned. Otherwise, it worked just like mixed.
      mode = JoinMode.ORDERED
    }

    mode match {
      case JoinMode.PRIORITY =>
        for (buf <- messages) {
          if (buf._1 == "source_1") {
            scheduler.receive(node, "result", buf._2)
          }
        }
      case JoinMode.ORDERED =>
        // This strikes me as not necessarily the most efficient thing...
        for (port <- ports.toList.sorted) {
          for (buf <- messages) {
            if (buf._1 == port) {
              scheduler.receive(node, "result", buf._2)
            }
          }
        }
      case JoinMode.MIXED =>
        for (buf <- messages) {
          scheduler.receive(node, "result", buf._2)
        }
    }

    messages.clear()
    ports.clear()
    scheduler.finish(node)
  }

  override def cleanup(): Unit = {
    super.cleanup()
    messages.clear()
    ports.clear()
  }
}
