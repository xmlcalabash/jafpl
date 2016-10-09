package com.jafpl.calc

import com.jafpl.items.NumberItem
import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.{Step, StepController}
import net.sf.saxon.s9api.QName
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * Created by ndw on 10/7/16.
  */
class Doubler() extends Step {
  var controller: StepController = _
  val logger = LoggerFactory.getLogger(this.getClass)
  var double = 0

  override def setup(controller: StepController, inputPorts: List[String], outputPorts: List[String], options: List[QName]): Unit = {
    this.controller = controller
  }

  override def reset(): Unit = {
    // nop
  }

  override def run(): Unit = {
    controller.send("result", new NumberItem(double))
  }

  override def teardown() = {
    // nop
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    var value = 0

    msg.item match {
      case num: NumberItem => value = num.get
      case _ => throw new CalcException("Message was not a number")
    }

    double = 2 * value
  }
}
