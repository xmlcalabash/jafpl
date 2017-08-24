package com.jafpl.steps

import com.jafpl.exceptions.PipelineException

class Uppercase extends DefaultStep {
  override def inputSpec = PortSpecification.SOURCE
  override def outputSpec = PortSpecification.RESULT

  override def receive(port: String, item: Any): Unit = {
    item match {
      case s: String =>
        consumer.get.send("result", s.toUpperCase())
      case _ =>
        throw new PipelineException("unexpectedtype", "Unexpected input type")
    }
  }
}
