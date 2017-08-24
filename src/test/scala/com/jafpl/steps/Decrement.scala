package com.jafpl.steps

import com.jafpl.exceptions.StepException

class Decrement() extends DefaultStep {
  override def inputSpec: PortSpecification = PortSpecification.SOURCE
  override def outputSpec: PortSpecification = PortSpecification.RESULT

  override def receive(port: String, item: Any): Unit = {
    item match {
      case num: Long => consumer.get.send("result", num - 1)
      case num: Int => consumer.get.send("result", num - 1)
      case _ => throw new StepException("nan", "Decrement input not a number: " + item)
    }
  }
}
