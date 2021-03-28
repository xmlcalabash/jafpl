package com.jafpl.steps

import com.jafpl.messages.{ItemMessage, Metadata}

class Producer(val items: List[Any]) extends DefaultStep {
  def this(str: Any) = {
    this(List(str))
  }

  override def inputSpec: PortSpecification = PortSpecification.NONE
  override def outputSpec: PortSpecification = PortSpecification.RESULTSEQ

  override def run(): Unit = {
    for (item <- items) {
      item match {
        case _: String => consumer.get.consume("result", new ItemMessage(item, Metadata.STRING))
        case _: Int => consumer.get.consume("result", new ItemMessage(item, Metadata.NUMBER))
        case _: Long => consumer.get.consume("result", new ItemMessage(item, Metadata.NUMBER))
        case _ => consumer.get.consume("result", new ItemMessage(item, Metadata.BLANK))
      }
    }
  }
}
