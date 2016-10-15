package com.jafpl.graph

import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.DefaultStep

/**
  * Created by ndw on 10/3/16.
  */
private[graph] class Fan(defLabel: String) extends DefaultStep {
  label = defLabel

  override def receive(port: String, msg: ItemMessage): Unit = {
    super.receive(port, msg)
    for (port <- outputPorts) {
      controller.send(port, msg.item)
    }
  }
}
