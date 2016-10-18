package com.jafpl.steps

import com.jafpl.items.NumberItem
import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.DefaultCompoundStep

import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/9/16.
  */
class IterateIntegers(list: List[Int]) extends DefaultCompoundStep {
  val integers = ListBuffer.empty[Int] ++= list
  label = "iter_int"

  override def run(): Unit = {
    if (integers.nonEmpty) {
      controller.send("current", new NumberItem(integers.head))
      controller.close("current")
      integers.remove(0)
    }
  }

  override def runAgain: Boolean = {
    integers.nonEmpty
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    // no inputs expected
  }
}
