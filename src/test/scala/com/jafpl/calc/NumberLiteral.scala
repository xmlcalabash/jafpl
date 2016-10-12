package com.jafpl.calc

import com.jafpl.items.NumberItem
import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.{DefaultStep, Step, StepController}
import net.sf.saxon.s9api.QName

/**
  * Created by ndw on 10/7/16.
  */
class NumberLiteral(val number: Int) extends DefaultStep {
  label = "num_lit"

  override def run(): Unit = {
    val item = new NumberItem(number)
    controller.send("result", item)
  }
}
