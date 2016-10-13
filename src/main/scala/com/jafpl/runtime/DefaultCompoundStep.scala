package com.jafpl.runtime

import com.jafpl.messages.ItemMessage

/**
  * Created by ndw on 10/7/16.
  */
abstract class DefaultCompoundStep extends DefaultStep with CompoundStep {
  def receiveOutput(port: String, msg: ItemMessage): Unit = {
    val outputPort = if (port.startsWith("I_")) {
      port.substring(2)
    } else {
      port
    }

    controller.send(outputPort, msg.item)
  }
}
