package com.jafpl.steps

import com.jafpl.messages.Metadata

class Producer(val items: List[Any]) extends DefaultStep {
  def this(str: Any) {
    this(List(str))
  }

  override def inputSpec: PortSpecification = PortSpecification.NONE
  override def outputSpec: PortSpecification = PortSpecification.RESULTSEQ

  override def run(): Unit = {
    for (item <- items) {
      item match {
        case str: String => consumer.get.send("result", item, Metadata.STRING)
        case num: Int => consumer.get.send("result", item, Metadata.NUMBER)
        case num: Long => consumer.get.send("result", item, Metadata.NUMBER)
        case _ => consumer.get.send("result", item, Metadata.BLANK)
      }
    }
  }
}
