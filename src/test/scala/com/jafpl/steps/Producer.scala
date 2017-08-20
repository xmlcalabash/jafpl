package com.jafpl.steps

class Producer(val items: List[String]) extends DefaultStep {
  override def inputSpec = PortBindingSpecification.NONE
  override def outputSpec = PortBindingSpecification.RESULTSEQ

  override def run(): Unit = {
    for (item <- items) {
      consumer.get.send("result", item)
    }
  }
}
