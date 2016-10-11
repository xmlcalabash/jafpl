package com.jafpl.calc

import com.jafpl.items.{GenericItem, NumberItem}
import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.{CompoundStep, DefaultCompoundStep, WhenStep}

/**
  * Created by ndw on 10/10/16.
  */
class WhenParity(name: String, private val chooseOdd: Boolean) extends DefaultCompoundStep(name) with WhenStep {
  override def test(item: GenericItem): Boolean = {
    val accept = item match {
      case num: NumberItem =>
        (num.get % 2 == 1 && chooseOdd) || (num.get %2 == 0 && !chooseOdd)
      case _ =>
        false
    }
    accept
  }

  override def runAgain: Boolean = false
}
