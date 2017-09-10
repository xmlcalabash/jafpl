package com.jafpl.steps

import com.jafpl.messages.{Message, Metadata}

class Duplicate(copies: Int) extends DefaultStep {
  override def inputSpec: PortSpecification = PortSpecification.SOURCE
  override def outputSpec: PortSpecification = PortSpecification.RESULTSEQ

  override def receive(port: String, message: Message): Unit = {
    for (count <- 1 to copies) {
      consumer.get.receive("result", message)
    }
  }
}
