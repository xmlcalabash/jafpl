package com.jafpl.runtime

import com.jafpl.items.GenericItem

/**
  * Created by ndw on 10/10/16.
  */
class WhenTrue() extends DefaultCompoundStep with WhenStep {
  label = "_when_true"
  override def test(item: GenericItem) = true
  override def runAgain: Boolean = false
}
