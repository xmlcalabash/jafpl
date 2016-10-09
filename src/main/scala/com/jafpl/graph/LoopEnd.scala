package com.jafpl.graph

import com.jafpl.items.GenericItem
import com.jafpl.messages.{CloseMessage, ItemMessage, RanMessage}
import com.jafpl.runtime.{CompoundEnd, CompoundStart}

import scala.collection.mutable

/**
  * Created by ndw on 10/2/16.
  */
class LoopEnd(graph: Graph, name: Option[String], step: CompoundEnd) extends Node(graph, name, Some(step)) {
}
