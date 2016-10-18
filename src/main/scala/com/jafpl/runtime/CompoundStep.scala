package com.jafpl.runtime

import com.jafpl.messages.ItemMessage

/**
  * Created by ndw on 10/10/16.
  */
trait CompoundStep extends Step {
  def caught(exception: Throwable): Boolean
  def receiveOutput(port: String, msg: ItemMessage)
  def runAgain: Boolean
}
