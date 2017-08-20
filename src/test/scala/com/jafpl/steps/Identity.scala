package com.jafpl.steps

class Identity extends DefaultStep {
  override def inputSpec = PortBindingSpecification.SOURCE
  override def outputSpec = PortBindingSpecification.RESULT

  override def receive(port: String, item: Any): Unit = {
    consumer.get.send("result", item)
  }
}
