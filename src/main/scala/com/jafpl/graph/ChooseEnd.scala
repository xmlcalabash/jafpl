package com.jafpl.graph

import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.CompoundStep
import com.jafpl.util.TreeWriter

/**
  * Created by ndw on 10/2/16.
  */
class ChooseEnd(graph: Graph, name: Option[String], step: Option[CompoundStep]) extends Node(graph, name, step) with CompoundEnd {
  var _chooseStart: ChooseStart = _

  def startNode = _chooseStart
  private[graph] def startNode_=(node: ChooseStart): Unit = {
    _chooseStart = node
  }

  def receiveOutput(port: String, msg: ItemMessage): Unit = {
    step.get.receiveOutput(port, msg)
  }

  final def runAgain = false

  override def dumpExtraAttr(tree: TreeWriter): Unit = {
    tree.addAttribute(Serializer._compound_start, _chooseStart.uid.toString)
  }
}
