package com.jafpl.steps

import com.jafpl.exceptions.PipelineException

class Decrement() extends DefaultStep {
  override def inputSpec: PortSpecification = PortSpecification.SOURCE
  override def outputSpec: PortSpecification = PortSpecification.RESULT

  override def receive(port: String, item: Any): Unit = {
    item match {
      case num: Long => consumer.get.send("result", num - 1)
      case num: Int => consumer.get.send("result", num - 1)
      case _ => throw new PipelineException("nam", "Decrement input not a number: " + item)
    }
  }
}
