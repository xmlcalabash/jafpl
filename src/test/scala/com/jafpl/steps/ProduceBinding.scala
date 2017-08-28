package com.jafpl.steps

import com.jafpl.messages.Metadata
import org.slf4j.{Logger, LoggerFactory}

class ProduceBinding(varname: String) extends DefaultStep {
  protected[jafpl] val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def inputSpec: PortSpecification = PortSpecification.NONE
  override def outputSpec: PortSpecification = PortSpecification.RESULT
  override def bindingSpec = new BindingSpecification(Set(varname))

  override def receiveBinding(varname: String, value: Any): Unit = {
    if (varname == this.varname) {
      consumer.get.receive("result", value, Metadata.BLANK)
    }
  }
}
