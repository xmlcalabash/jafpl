package com.jafpl.steps

import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.DefaultStep

/**
  * Created by ndw on 10/17/16.
  */
class Bang() extends DefaultStep {
  label = "Bang"

  override def run(): Unit = {
    throw new RuntimeException("Bang!")
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    // nop
  }
}
