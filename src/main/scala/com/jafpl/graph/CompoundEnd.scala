package com.jafpl.graph

import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.StepController

/**
  * Created by ndw on 10/10/16.
  */
trait CompoundEnd extends StepController {
  def startNode: CompoundStart
  def receiveOutput(port: String, msg: ItemMessage)
  def runAgain: Boolean
}