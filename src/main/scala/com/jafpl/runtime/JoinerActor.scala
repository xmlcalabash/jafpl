package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{JoinMode, Joiner, Node}
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphMonitor.GOutput
import com.jafpl.util.UniqueId

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class JoinerActor(private val monitor: ActorRef,
                                   override protected val runtime: GraphRuntime,
                                   override protected val node: Joiner) extends NodeActor(monitor, runtime, node) {
  private val id = UniqueId.nextId
  private val portClosed = mutable.HashMap.empty[Int,Boolean]
  private val portBuffer = mutable.HashMap.empty[Int,ListBuffer[Message]]
  private var currentPort = 1
  private var hadPriorityInput = false
  private var hadGatingInput = false
  logEvent = TraceEvent.JOINER

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", logEvent)
    if (node.mode == JoinMode.MIXED) {
      monitor ! GOutput(node, "result", item)
    } else {
      this.synchronized {
        orderedInput(from, fromPort, port, item)
      }
    }
  }

  override protected def close(port: String): Unit = {
    trace("CLOSE", s"$node", logEvent)
    if (node.mode != JoinMode.MIXED) {
      this.synchronized {
        portClosed.put(portNo(port),true)
        drainBuffers()
      }
    } else {
      super.close(port)
    }
  }

  private def portNo(port: String): Int = {
    // port = "source_[nnn]"
    port.substring(7).toInt
  }

  private def orderedInput(from: Node, fromPort: String, port: String, item: Message): Unit = {
    val pnum = portNo(port)

    if (!hadPriorityInput) {
      hadPriorityInput = (node.mode == JoinMode.PRIORITY && pnum == 1)
    }

    var writeOk = writeMessage(port, item)

    if (writeOk) {
      monitor ! GOutput(node, "result", item)
      return
    }

    if (pnum < currentPort) {
      throw new IllegalArgumentException(s"Received input from previous port: $id: $pnum")
    }

    drainBuffers()

    // If we got here, then pnum > 1 and we've just drained all the queued
    // messages that we can. If pnum == currentPort *now*, then we can
    // write it, subject to the priority constraints

    writeOk = writeMessage(port, item)

    if (writeOk) {
      monitor ! GOutput(node, "result", item)
    } else {
      val list = portBuffer.getOrElse(pnum, ListBuffer.empty[Message])
      list += item
      portBuffer.put(pnum, list)
    }
  }

  private def writeMessage(port: String, item: Message): Boolean = {
    val pnum = portNo(port)

    var writeOk = (pnum == currentPort)

    if (currentPort != 1) {
      node.mode match {
        case JoinMode.PRIORITY =>
          writeOk = writeOk && !hadPriorityInput
        case _ => Unit
      }
    }

    writeOk
  }

  private def drainBuffers(): Unit = {
    var port = currentPort
    var stillDraining = portClosed.getOrElse(port, false)
    while (stillDraining) {
      stillDraining = portClosed.getOrElse(port, false)
      val list = portBuffer.getOrElse(port, ListBuffer.empty[Message])

      val allowWrite = if (port == 1) {
        true
      } else {
        node.mode match {
          case JoinMode.PRIORITY =>
            !hadPriorityInput
          case _ =>
            true
        }
      }

      if (allowWrite) {
        for (item <- list) {
          monitor ! GOutput(node, "result", item)
        }
      }

      portBuffer.remove(port)
      if (stillDraining) {
        super.close("source_" + port)
        port += 1
      }
    }
    currentPort = port
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Joiner]"
  }
}
