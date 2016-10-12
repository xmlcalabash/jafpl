package com.jafpl.calc

import com.jafpl.items.NumberItem
import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.{Step, StepController}
import net.sf.saxon.s9api.QName
import org.slf4j.LoggerFactory

/**
  * Created by ndw on 10/7/16.
  */
class UnaryExpr(val op: String) extends Step {
  var controller: StepController = _
  val logger = LoggerFactory.getLogger(this.getClass)
  var _label = "unknown"

  override def label = _label
  override def label_=(label: String): Unit = {
    _label = label
  }

  override def setup(controller: StepController, inputPorts: List[String], outputPorts: List[String]): Unit = {
    this.controller = controller
  }

  override def reset(): Unit = {
    // nop
  }

  override def run(): Unit = {
    // nop
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

    if (op == "-" && value < 0) {
      logger.info("{}: {}({})", this, op, value.toString)
    } else {
      logger.info("{}: {}{}", this, op, value.toString)
    }

    if (op == "+") {
      controller.send("result", new NumberItem(value))
    } else {
      controller.send("result", new NumberItem(-value))
    }
  }
}
