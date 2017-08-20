package com.jafpl.steps

class Count extends DefaultStep {
  var count: Long = 0

  override def inputSpec = PortBindingSpecification.SOURCE
  override def outputSpec = PortBindingSpecification.RESULT

  override def receive(port: String, item: Any): Unit = {
    count += 1
  }

  override def run(): Unit = {
    consumer.get.send("result", count.toString)
  }
}
