package com.jafpl.steps

class Producer(val items: List[Any]) extends DefaultStep {
  def this(str: Any) {
    this(List(str))
  }

  override def inputSpec = PortBindingSpecification.NONE
  override def outputSpec = PortBindingSpecification.RESULTSEQ

  override def run(): Unit = {
    for (item <- items) {
      consumer.get.send("result", item)
    }
  }
}
