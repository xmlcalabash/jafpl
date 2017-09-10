package com.jafpl.steps

import com.jafpl.messages.{ItemMessage, Message, Metadata}

class Count extends DefaultStep {
  var count: Long = 0

  override def inputSpec: PortSpecification = PortSpecification.SOURCESEQ
  override def outputSpec: PortSpecification = PortSpecification.RESULT

  override def receive(port: String, message: Message): Unit = {
    count += 1
  }

  override def run(): Unit = {
    consumer.get.receive("result", new ItemMessage(count, Metadata.BLANK))
  }
}
