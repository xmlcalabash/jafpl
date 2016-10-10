package com.jafpl.graph

import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.CompoundStep
import com.jafpl.util.TreeWriter
import net.sf.saxon.s9api.QName

/**
  * Created by ndw on 10/2/16.
  */
class WhenEnd(graph: Graph, name: Option[String], step: Option[CompoundStep]) extends Node(graph, name, step) with CompoundEnd {
  var _whenStart: WhenStart = _

  def startNode = _whenStart
  private[graph] def startNode_=(node: WhenStart): Unit = {
    _whenStart = node
  }

  def receiveOutput(port: String, msg: ItemMessage): Unit = {
    step.get.receiveOutput(port, msg)
  }

  final def runAgain = false

  override def dumpExtraAttr(tree: TreeWriter): Unit = {
    tree.addAttribute(Serializer._compound_start, _whenStart.uid.toString)
  }
}
