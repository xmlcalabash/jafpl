package com.jafpl.steps

class Duplicate(copies: Int) extends DefaultStep {
  override def inputSpec = PortSpecification.SOURCE
  override def outputSpec = PortSpecification.RESULTSEQ

  override def receive(port: String, item: Any): Unit = {
    for (count <- 1 to copies) {
      consumer.get.send("result", item)
    }
  }
}
