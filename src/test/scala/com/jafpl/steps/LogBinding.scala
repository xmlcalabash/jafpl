package com.jafpl.steps

import org.slf4j.LoggerFactory

class LogBinding() extends DefaultStep {
  protected[jafpl] val logger = LoggerFactory.getLogger(this.getClass)
  private var message: String = ""

  override def inputSpec = PortBindingSpecification.NONE
  override def outputSpec = PortBindingSpecification.RESULT
  override def requiredBindings = Set("message")

  override def receiveBinding(varname: String, value: Any): Unit = {
    if (varname == "message") {
      message = value.toString
    }
  }

  override def run(): Unit = {
    logger.info("Message binding: " + message)
  }
}
