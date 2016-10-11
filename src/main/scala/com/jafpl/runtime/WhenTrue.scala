package com.jafpl.runtime

import com.jafpl.items.GenericItem

/**
  * Created by ndw on 10/10/16.
  */
class WhenTrue() extends DefaultCompoundStep("when-true") with WhenStep {
  override def test(item: GenericItem) = true
  override def runAgain: Boolean = false
}
