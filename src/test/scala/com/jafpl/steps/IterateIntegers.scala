package com.jafpl.steps

import com.jafpl.items.{NumberItem, StringItem}
import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.DefaultCompoundStep

import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/9/16.
  */
class IterateIntegers() extends DefaultCompoundStep {
  val list = ListBuffer.empty[Int]
  var current = 0
  var max = 0
  label = "iter_int"

  override def run(): Unit = {
    current += 1
    list += current
    //logger.info(s"Iterate: $current")
    controller.send("current", new NumberItem(current))
    controller.close("current")
  }

  override def teardown(): Unit = {
    for (item <- list) {
      println("Generated " + item)
    }
  }

  override def runAgain: Boolean = {
    val again = (current < max) && (max > 0)
    again
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
