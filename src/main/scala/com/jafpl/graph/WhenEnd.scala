package com.jafpl.graph

import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.{CompoundStep, Whener}
import com.jafpl.util.XmlWriter

/**
  * Created by ndw on 10/2/16.
  */
class WhenEnd(graph: Graph, step: Option[CompoundStep]) extends Node(graph, step) with CompoundEnd {
  var _whenStart: WhenStart = _
  label = Some("_when_end")

  def startNode = _whenStart
  private[graph] def startNode_=(node: WhenStart): Unit = {
    _whenStart = node
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    val outputPort = if (port.startsWith("I_")) {
      port.substring(2)
    } else {
      port
    }
    step.get.receiveOutput(port, msg)
  }

  override def receiveOutput(port: String, msg: ItemMessage): Unit = {
    logger.warn("receiveOutput called on WhenEnd")
  }

  final def runAgain = false

  override def dumpExtraAttr(tree: XmlWriter): Unit = {
    tree.addAttribute(Serializer._compound_start, _whenStart.uid.toString)
  }
}
