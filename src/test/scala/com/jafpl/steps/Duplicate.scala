package com.jafpl.steps

class Duplicate(copies: Int) extends DefaultStep {
  override def inputSpec = PortBindingSpecification.SOURCE
  override def outputSpec = PortBindingSpecification.RESULTSEQ

  override def receive(port: String, item: Any): Unit = {
    for (count <- 1 to copies) {
      consumer.get.send("result", item)
    }
  }
}
