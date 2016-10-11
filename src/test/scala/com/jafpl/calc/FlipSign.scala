package com.jafpl.calc

import com.jafpl.items.NumberItem
import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.DefaultStep

/**
  * Created by ndw on 10/7/16.
  */
class FlipSign() extends DefaultStep("flipsign") {
  var inputNumber = 0

  override def run(): Unit = {
    controller.send("result", new NumberItem(- inputNumber))
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    msg.item match {
      case num: NumberItem => inputNumber = num.get
      case _ => throw new CalcException("Message was not a number")
    }
  }
}
