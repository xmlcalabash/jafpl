package com.jafpl.graph

import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.{DefaultStep, Step, StepController}

/**
  * Created by ndw on 10/3/16.
  */
private[graph] class Fan(name: String) extends DefaultStep(name) {
  override def receive(port: String, msg: ItemMessage): Unit = {
    super.receive(port, msg)
    for (port <- outputPorts) {
      controller.send(port, msg.item)
    }
  }
}
