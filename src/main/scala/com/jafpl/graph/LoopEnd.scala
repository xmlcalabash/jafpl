package com.jafpl.graph

import com.jafpl.items.GenericItem
import com.jafpl.messages.{CloseMessage, ItemMessage, RanMessage}
import com.jafpl.runtime.CompoundStep
import com.jafpl.util.TreeWriter

import scala.collection.mutable

/**
  * Created by ndw on 10/2/16.
  */
class LoopEnd(graph: Graph, name: Option[String], step: Option[CompoundStep]) extends Node(graph, name, step) with CompoundEnd {
  var _loopStart: LoopStart = _

  def startNode = _loopStart
  private[graph] def startNode_=(node: LoopStart): Unit = {
    _loopStart = node
  }

  def receiveOutput(port: String, msg: ItemMessage): Unit = {
    step.get.receiveOutput(port, msg)
  }

  def runAgain: Boolean = {
    step.get.runAgain
  }

  override def dumpExtraAttr(tree: TreeWriter): Unit = {
    tree.addAttribute(Serializer._compound_start, _loopStart.uid.toString)
  }
}
