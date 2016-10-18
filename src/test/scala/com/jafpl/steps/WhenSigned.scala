package com.jafpl.steps

import com.jafpl.items.{GenericItem, NumberItem}
import com.jafpl.runtime.{DefaultCompoundStep, WhenStep}

/**
  * Created by ndw on 10/10/16.
  */
class WhenSigned(private val choosePos: Boolean) extends DefaultCompoundStep with WhenStep {
  label = if (choosePos) {
    "when_pos"
  } else {
    "when_neg"
  }

  override def test(item: GenericItem): Boolean = {
    val accept = item match {
      case num: NumberItem =>
        ((num.get > 0) && choosePos) || ((num.get < 0) && !choosePos)
      case _ =>
        false
    }
    accept
  }

  override def runAgain: Boolean = false
}
