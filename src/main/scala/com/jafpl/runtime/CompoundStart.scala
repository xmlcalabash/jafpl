package com.jafpl.runtime

import com.jafpl.graph.Node
import com.jafpl.messages.ItemMessage

/**
  * Created by ndw on 10/8/16.
  */
trait CompoundStart extends Step {
  def compoundEnd: CompoundEnd
  def subpipeline: List[Node]
  def readyToRestart()
  def finished: Boolean
  def completed: Boolean
  def receiveResult(port: String, msg: ItemMessage)
}
