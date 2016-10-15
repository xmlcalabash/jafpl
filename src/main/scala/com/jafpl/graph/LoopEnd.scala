package com.jafpl.graph

import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.CompoundStep
import com.jafpl.util.XmlWriter

/**
  * Created by ndw on 10/2/16.
  */
class LoopEnd(graph: Graph, step: Option[CompoundStep]) extends Node(graph, step) with CompoundEnd {
  var _loopStart: LoopStart = _
  label = Some("_loop_end")

  def startNode = _loopStart
  private[graph] def startNode_=(node: LoopStart): Unit = {
    _loopStart = node
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
    logger.warn("receiveOutput called on LoopEnd")
  }

  def runAgain: Boolean = {
    step.get.runAgain
  }

  override def dumpExtraAttr(tree: XmlWriter): Unit = {
    tree.addAttribute(Serializer._compound_start, _loopStart.uid.toString)
  }
}
