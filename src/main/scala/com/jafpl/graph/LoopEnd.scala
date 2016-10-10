package com.jafpl.graph

import com.jafpl.items.GenericItem
import com.jafpl.messages.{CloseMessage, ItemMessage, RanMessage}
import com.jafpl.runtime.CompoundStep

import scala.collection.mutable

/**
  * Created by ndw on 10/2/16.
  */
class LoopEnd(graph: Graph, name: Option[String], step: CompoundStep) extends Node(graph, name, Some(step)) {
  var _loopStart: LoopStart = _

  def loopStart = _loopStart
  private[graph] def loopStart_=(node: LoopStart): Unit = {
    _loopStart = node
  }

  def receiveOutput(port: String, msg: ItemMessage): Unit = {
    step.receiveOutput(port, msg)
  }

  def runAgain: Boolean = {
    step.runAgain
  }
}
