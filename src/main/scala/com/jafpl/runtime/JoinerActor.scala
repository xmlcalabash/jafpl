package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{JoinMode, Joiner, Node}
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class JoinerActor(private val monitor: ActorRef,
                                   override protected val runtime: GraphRuntime,
                                   override protected val node: Joiner) extends NodeActor(monitor, runtime, node) {
  // Joiner used to try to be clever, forwarding on messages as quickly as they arrived
  // after assuring they were in the right order. This rewrite removes that feature.
  // It now buffers until it has all the inputs. Motivation for this change is that
  // a joiner inside a When that forwards messages before it's asked to run appears
  // to cause extra messages to "leak" out of the When. Well, that's my current best
  // guess for the odd behavior I'm seeing on ab-for-each-003.xml from the XProc 3.0
  // test suite. //ndw 5 Aug 2019
  private val buffered = mutable.HashMap.empty[String, ListBuffer[Message]]
  logEvent = TraceEvent.JOINER

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    if (buffered.contains(port)) {
      buffered(port) += item
    } else {
      val mbuf = ListBuffer.empty[Message]
      mbuf += item
      buffered(port) = mbuf
    }
    println(s"Joiner $node received: $buffered")
  }

  override protected def reset(): Unit = {
    super.reset()
    buffered.clear()
    println(s"Joiner $node reset")
  }

  override protected def run(): Unit = {
    trace("RUN", s"$node", logEvent)

    readyToRun = false

    println(s"Joiner $node runs ${node.mode}")
    if (node.mode == JoinMode.PRIORITY && buffered.contains("source_1")) {
      val port = s"source_1"
      for (item <- buffered(port)) {
        monitor ! GOutput(node, "result", item)
      }
    } else {
      println(s"Joiner $node: $buffered")
      var portNum = 0
      while (buffered.nonEmpty) {
        portNum += 1
        val port = s"source_$portNum"
        if (buffered.contains(port)) {
          for (item <- buffered(port)) {
            println(s"Joiner $node/$port sends $item")
            monitor ! GOutput(node, "result", item)
          }
          buffered.remove(port)
        }
      }
    }

    for (output <- node.outputs) {
      monitor ! GClose(node, output)
    }
    monitor ! GFinished(node)
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Joiner]"
  }
}
