package com.jafpl.steps

import org.slf4j.LoggerFactory

class ProduceBinding(varname: String) extends DefaultStep {
  protected[jafpl] val logger = LoggerFactory.getLogger(this.getClass)

  override def inputSpec = PortBindingSpecification.NONE
  override def outputSpec = PortBindingSpecification.RESULT
  override def requiredBindings = Set(varname)

  override def receiveBinding(varname: String, value: Any): Unit = {
    if (varname == this.varname) {
      consumer.get.send("result", value)
    }
  }
}
