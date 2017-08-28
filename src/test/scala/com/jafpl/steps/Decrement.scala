package com.jafpl.steps

import com.jafpl.exceptions.StepException
import com.jafpl.messages.Metadata

class Decrement() extends DefaultStep {
  override def inputSpec: PortSpecification = PortSpecification.SOURCE
  override def outputSpec: PortSpecification = PortSpecification.RESULT

  override def receive(port: String, item: Any, metadata: Metadata): Unit = {
    item match {
      case num: Long => consumer.get.send("result", num - 1, Metadata.NUMBER)
      case num: Int => consumer.get.send("result", num - 1, Metadata.NUMBER)
      case _ => throw new StepException("nan", "Decrement input not a number: " + item)
    }
  }
}
