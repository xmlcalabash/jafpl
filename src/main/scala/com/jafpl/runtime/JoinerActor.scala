package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{JoinMode, Joiner, Node}
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphMonitor.GOutput
import com.jafpl.util.UniqueId

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class JoinerActor(private val monitor: ActorRef,
                                   private val runtime: GraphRuntime,
                                   private val node: Joiner) extends NodeActor(monitor, runtime, node) {
  private val id = UniqueId.nextId
  private val portClosed = mutable.HashMap.empty[Int,Boolean]
  private val portBuffer = mutable.HashMap.empty[Int,ListBuffer[Message]]
  private var currentPort = 1
  private var hadPriorityInput = false

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    if (node.mode == JoinMode.MIXED) {
      monitor ! GOutput(node, "result", item)
    } else {
      this.synchronized {
        orderedInput(from, fromPort, port, item)
      }
    }
  }

  override protected def close(port: String): Unit = {
    if (node.mode != JoinMode.MIXED) {
      this.synchronized {
        portClosed.put(portNo(port),true)
        drainBuffers()
      }
      super.close(port)
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
    if (pnum == currentPort) {
      hadPriorityInput = hadPriorityInput || (node.mode == JoinMode.PRIORITY && currentPort == 1)
      monitor ! GOutput(node, "result", item)
      return
    }

    if (pnum < currentPort) {
      throw new IllegalArgumentException(s"Received input from previous port: $id: $pnum")
    }

    drainBuffers()

    val allowWrite = (node.mode != JoinMode.PRIORITY) || (currentPort == 1)

    if (allowWrite && (pnum == currentPort)) {
      monitor ! GOutput(node, "result", item)
    } else {
      val list = portBuffer.getOrElse(pnum, ListBuffer.empty[Message])
      list += item
      portBuffer.put(pnum, list)
    }
  }

  private def drainBuffers(): Unit = {
    var port = currentPort
    var stillDraining = portClosed.getOrElse(port, false)
    while (stillDraining) {
      stillDraining = portClosed.getOrElse(port, false)
      val list = portBuffer.getOrElse(port, ListBuffer.empty[Message])

      val allowWrite = (node.mode != JoinMode.PRIORITY) || (port == 1) || (!hadPriorityInput && (port == 2))
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
}
