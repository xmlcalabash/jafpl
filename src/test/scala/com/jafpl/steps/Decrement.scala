package com.jafpl.steps

import com.jafpl.exceptions.PipelineException

class Decrement() extends DefaultStep {
  override def inputSpec: PortBindingSpecification = PortBindingSpecification.SOURCE
  override def outputSpec: PortBindingSpecification = PortBindingSpecification.RESULT

  override def receive(port: String, item: Any): Unit = {
    item match {
      case num: Long => consumer.get.send("result", num - 1)
      case num: Int => consumer.get.send("result", num - 1)
      case _ => throw new PipelineException("nam", "Decrement input not a number: " + item)
    }
  }
}
