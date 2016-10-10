package com.jafpl.graph

import com.jafpl.runtime.StepController

/**
  * Created by ndw on 10/10/16.
  */
trait CompoundStart extends StepController {
  def endNode: CompoundEnd
  def runAgain: Boolean
}
