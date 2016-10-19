package com.jafpl.runtime

import com.jafpl.graph.{CompoundEnd, Graph, Node, Serializer}
import com.jafpl.messages.ItemMessage
import com.jafpl.util.XmlWriter

/**
  * Created by ndw on 10/16/16.
  */
class DefaultCompoundEnd(graph: Graph, step: Option[CompoundStep]) extends Node(graph, None) with CompoundEnd {
  private var _start: Node = _
  private val _step = step

  def compoundStart = _start
  private[jafpl] def compoundStart_=(node: Node): Unit = {
    _start = node
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    println("LOOP END RECEIVES " + msg)
    val outputPort = if (port.startsWith("I_")) {
      port.substring(2)
    } else {
      port
    }
    _step.get.receiveOutput(port, msg)
  }

  override def receiveOutput(port: String, msg: ItemMessage): Unit = {
    logger.warn("receiveOutput called on CompoundEnd")
  }

  override def dumpExtraAttr(tree: XmlWriter): Unit = {
    // This is a hack; I expect all of the compoundEnds to be extensions of Node
    if (_start.isInstanceOf[Node]) {
      tree.addAttribute(Serializer._compound_start, _start.asInstanceOf[Node].uid.toString)
    }
  }
}
