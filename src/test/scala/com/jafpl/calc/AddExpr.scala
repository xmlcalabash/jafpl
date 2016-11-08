package com.jafpl.calc

import com.jafpl.items.NumberItem
import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.{Step, StepController}
import com.jafpl.util.SourceLocation
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * Created by ndw on 10/7/16.
  */
class AddExpr(val ops: List[String]) extends Step {
  var controller: StepController = _
  val logger = LoggerFactory.getLogger(this.getClass)
  val operands = mutable.HashMap.empty[String, Int]
  var _label = "add"
  var _location: Option[SourceLocation] = None

  override def label = _label
  def label_=(label: String): Unit = {
    _label = label
  }

  override def location = _location
  def location_=(sourceLocation: SourceLocation): Unit = {
    _location = Some(sourceLocation)
  }

  override def setup(controller: StepController, inputPorts: List[String], outputPorts: List[String]): Unit = {
    this.controller = controller
  }

  override def reset(): Unit = {
    //logger.info("AddExpr run")
    operands.clear()
  }

  override def run(): Unit = {
    //logger.info("AddExpr run")
    var acc = operands("s1")
    var pos = 2
    for (op <- ops) {
      val operand = operands("s" + pos)
      //logger.info(s"AddExpr: $acc $op $operand")
      op match {
        case "+" => acc = acc + operand
        case "-" => acc = acc - operand
      }
      pos += 1
    }
    val item = new NumberItem(acc)
    controller.send("result", item)
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

    //logger.info("AddExpr received {}: {}", port, value)

    operands.put(port, value)
  }
}
