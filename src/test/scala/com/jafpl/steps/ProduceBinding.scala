package com.jafpl.steps

import com.jafpl.messages.BindingMessage
import org.slf4j.{Logger, LoggerFactory}

class ProduceBinding(varname: String) extends DefaultStep {
  protected[jafpl] val logger: Logger = LoggerFactory.getLogger(this.getClass)

  override def inputSpec: PortSpecification = PortSpecification.NONE
  override def outputSpec: PortSpecification = PortSpecification.RESULT
  override def bindingSpec = new BindingSpecification(Set(varname))

  override def receiveBinding(message: BindingMessage): Unit = {
    if (varname == message.name) {
      consumer.get.consume("result", message.message)
    }
  }
}
