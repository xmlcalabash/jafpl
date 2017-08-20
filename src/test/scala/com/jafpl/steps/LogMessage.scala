package com.jafpl.steps

import org.slf4j.LoggerFactory

class LogMessage(val message: String) extends DefaultStep {
  protected[jafpl] val logger = LoggerFactory.getLogger(this.getClass)

  override def inputSpec = PortBindingSpecification.NONE
  override def outputSpec = PortBindingSpecification.RESULT

  override def run(): Unit = {
    logger.info(message)
  }
}
