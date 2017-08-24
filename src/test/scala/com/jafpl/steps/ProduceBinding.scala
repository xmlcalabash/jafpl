package com.jafpl.steps

import org.slf4j.LoggerFactory

class ProduceBinding(varname: String) extends DefaultStep {
  protected[jafpl] val logger = LoggerFactory.getLogger(this.getClass)

  override def inputSpec = PortSpecification.NONE
  override def outputSpec = PortSpecification.RESULT
  override def bindingSpec = new BindingSpecification(Set(varname))

  override def receiveBinding(varname: String, value: Any): Unit = {
    if (varname == this.varname) {
      consumer.get.send("result", value)
    }
  }
}
