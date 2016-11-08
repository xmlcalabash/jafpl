package com.jafpl.steps

import com.jafpl.items.{NumberItem, StringItem}
import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.DefaultCompoundStep

/**
  * Created by ndw on 10/9/16.
  */
class IterateIntegers() extends DefaultCompoundStep {
  var current = 1
  var max = 0
  label = "iter_int"

  override def run(): Unit = {
    logger.debug("Send {}: {}", current, this)
    controller.send("current", new NumberItem(current))
    current += 1
  }

  override def runAgain: Boolean = {
    logger.debug("Again? {} <= {}", current, max)
    (current <= max) && (max > 0)
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    if (port == "source") {
      msg.item match {
        case n: NumberItem => max = n.get
        case s: StringItem => max = s.get.toInt
      }
    }
  }
}
