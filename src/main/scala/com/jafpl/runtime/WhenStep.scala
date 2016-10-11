package com.jafpl.runtime

import com.jafpl.items.GenericItem

/**
  * Created by ndw on 10/10/16.
  */
trait WhenStep extends CompoundStep {
  def test(msg: GenericItem): Boolean
}
