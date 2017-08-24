package com.jafpl.steps

class Sleep(val millis: Long) extends DefaultStep {
  override def inputSpec = PortSpecification.NONE
  override def outputSpec = PortSpecification.NONE

  override def run(): Unit = {
    Thread.sleep(millis)
  }
}
