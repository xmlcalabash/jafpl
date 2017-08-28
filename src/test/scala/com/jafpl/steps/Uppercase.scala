package com.jafpl.steps

import com.jafpl.exceptions.StepException
import com.jafpl.messages.Metadata

class Uppercase extends DefaultStep {
  override def inputSpec: PortSpecification = PortSpecification.SOURCE
  override def outputSpec: PortSpecification = PortSpecification.RESULT

  override def receive(port: String, item: Any, metadata: Metadata): Unit = {
    item match {
      case s: String =>
        consumer.get.send("result", s.toUpperCase(), Metadata.STRING)
      case _ =>
        throw new StepException("unexpectedtype", "Unexpected input type")
    }
  }
}
