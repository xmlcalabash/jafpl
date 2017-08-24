package com.jafpl.steps

class Producer(val items: List[Any]) extends DefaultStep {
  def this(str: Any) {
    this(List(str))
  }

  override def inputSpec = PortSpecification.NONE
  override def outputSpec = PortSpecification.RESULTSEQ

  override def run(): Unit = {
    for (item <- items) {
      consumer.get.send("result", item)
    }
  }
}
