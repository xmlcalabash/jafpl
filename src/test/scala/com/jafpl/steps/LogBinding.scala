package com.jafpl.steps

import com.jafpl.messages.BindingMessage
import org.slf4j.{Logger, LoggerFactory}

class LogBinding() extends DefaultStep {
  protected[jafpl] val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private var message: String = ""

  override def inputSpec: PortSpecification = PortSpecification.NONE
  override def outputSpec: PortSpecification = PortSpecification.RESULT
  override def bindingSpec = new BindingSpecification(Set("message"))

  override def receiveBinding(bindmsg: BindingMessage): Unit = {
    if (bindmsg.name == "message") {
      message = bindmsg.message.toString
    }
  }

  override def run(): Unit = {
    logger.info("Message binding: " + message)
  }
}
