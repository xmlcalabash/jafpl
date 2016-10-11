package com.jafpl.calc

import com.jafpl.graph.Node
import com.jafpl.items.NumberItem
import com.jafpl.messages.{ItemMessage, ResetMessage}
import com.jafpl.runtime.DefaultCompoundStep

import scala.collection.mutable

/**
  * Created by ndw on 10/9/16.
  */
class IterateIntegers(name: String) extends DefaultCompoundStep(name) {
  val integers = mutable.HashMap.empty[Int, Int]
  var index = 1

  override def run(): Unit = {
    if (integers.contains(index)) {
      controller.send("current", new NumberItem(integers(index) * 2))
      controller.close("current")
      index += 1
    }
  }

  override def runAgain: Boolean = {
    integers.contains(index+1)
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    val index = port.substring(1).toInt

    msg.item match {
      case num: NumberItem =>
        integers.put(index, num.get)
      case _ => throw new CalcException("Message was not a number")
    }
  }
}
