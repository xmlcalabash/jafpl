package com.jafpl.steps

import com.jafpl.calc.CalcException
import com.jafpl.items.NumberItem
import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.DefaultStep

/**
  * Created by ndw on 10/7/16.
  */
class Doubler() extends DefaultStep {
  var inputNumber = 0
  label = "Doubler"

  override def run(): Unit = {
    controller.send("result", new NumberItem(inputNumber * 2))
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    msg.item match {
      case num: NumberItem => inputNumber = num.get
      case _ => throw new CalcException("Message was not a number")
    }
  }
}
