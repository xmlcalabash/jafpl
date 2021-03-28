package com.jafpl.steps

import com.jafpl.messages.{ItemMessage, Message, Metadata}
import com.jafpl.util.UniqueId

class Count extends DefaultStep {
  private var count: Long = 0

  override def inputSpec: PortSpecification = PortSpecification.SOURCESEQ
  override def outputSpec: PortSpecification = PortSpecification.RESULT

  override def consume(port: String, message: Message): Unit = {
    count += 1
  }

  override def run(): Unit = {
    consumer.get.consume("result", new ItemMessage(count, Metadata.BLANK))
  }

  override def reset(): Unit = {
    super.reset()
    count = 0
  }
}
