package com.jafpl.steps

import com.jafpl.messages.{ItemMessage, Metadata}

class Producer(val items: List[Any]) extends DefaultStep {
  def this(str: Any) {
    this(List(str))
  }

  override def inputSpec: PortSpecification = PortSpecification.NONE
  override def outputSpec: PortSpecification = PortSpecification.RESULTSEQ

  override def run(): Unit = {
    for (item <- items) {
      item match {
        case str: String => consumer.get.receive("result", new ItemMessage(item, Metadata.STRING))
        case num: Int => consumer.get.receive("result", new ItemMessage(item, Metadata.NUMBER))
        case num: Long => consumer.get.receive("result", new ItemMessage(item, Metadata.NUMBER))
        case _ => consumer.get.receive("result", new ItemMessage(item, Metadata.BLANK))
      }
    }
  }
}
