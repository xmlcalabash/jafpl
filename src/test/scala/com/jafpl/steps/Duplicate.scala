package com.jafpl.steps

import com.jafpl.messages.Metadata

class Duplicate(copies: Int) extends DefaultStep {
  override def inputSpec: PortSpecification = PortSpecification.SOURCE
  override def outputSpec: PortSpecification = PortSpecification.RESULTSEQ

  override def receive(port: String, item: Any, metadata: Metadata): Unit = {
    for (count <- 1 to copies) {
      consumer.get.send("result", item, metadata)
    }
  }
}
