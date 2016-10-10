package com.jafpl.calc

import com.jafpl.items.NumberItem
import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.{DefaultStep, Step, StepController}
import net.sf.saxon.s9api.QName
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * Created by ndw on 10/7/16.
  */
class Doubler() extends DefaultStep("doubler") {
  var inputNumber = 0

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
