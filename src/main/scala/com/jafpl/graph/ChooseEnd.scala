package com.jafpl.graph

import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.CompoundStep
import com.jafpl.util.XmlWriter

/**
  * Created by ndw on 10/2/16.
  */
class ChooseEnd(graph: Graph, step: Option[CompoundStep]) extends Node(graph, step) with CompoundEnd {
  var _chooseStart: ChooseStart = _
  label = Some("_choose_end")

  def startNode = _chooseStart
  private[graph] def startNode_=(node: ChooseStart): Unit = {
    _chooseStart = node
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
    logger.warn("receiveOutput called on ChooseEnd")
  }

  final def runAgain = false

  override def dumpExtraAttr(tree: XmlWriter): Unit = {
    tree.addAttribute(Serializer._compound_start, _chooseStart.uid.toString)
  }
}
