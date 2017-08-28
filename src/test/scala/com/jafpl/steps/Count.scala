package com.jafpl.steps

import com.jafpl.messages.Metadata

class Count extends DefaultStep {
  var count: Long = 0

  override def inputSpec: PortSpecification = PortSpecification.SOURCESEQ
  override def outputSpec: PortSpecification = PortSpecification.RESULT

  override def receive(port: String, item: Any, metadata: Metadata): Unit = {
    count += 1
  }

  override def run(): Unit = {
    consumer.get.receive("result", count, Metadata.BLANK)
  }
}
