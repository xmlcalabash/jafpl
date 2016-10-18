package com.jafpl.steps

import com.jafpl.items.{GenericItem, NumberItem}
import com.jafpl.runtime.{DefaultCompoundStep, WhenStep}

/**
  * Created by ndw on 10/10/16.
  */
class WhenParity(private val chooseOdd: Boolean) extends DefaultCompoundStep with WhenStep {
  label = if (chooseOdd) {
    "when_odd"
  } else {
    "when_even"
  }

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
