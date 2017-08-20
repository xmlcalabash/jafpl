package com.jafpl.steps

class Sleep(val millis: Long) extends DefaultStep {
  override def inputSpec = PortBindingSpecification.NONE
  override def outputSpec = PortBindingSpecification.NONE

  override def run(): Unit = {
    Thread.sleep(millis)
  }
}
