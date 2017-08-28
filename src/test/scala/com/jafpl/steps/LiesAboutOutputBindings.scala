package com.jafpl.steps

import com.jafpl.messages.Metadata

class LiesAboutOutputBindings extends DefaultStep {
  override def inputSpec: PortSpecification = PortSpecification.SOURCESEQ
  override def outputSpec: PortSpecification = PortSpecification.RESULT

  override def run(): Unit = {
    consumer.get.receive("result", "one", Metadata.STRING)
    consumer.get.receive("result", "two", Metadata.STRING) // but we asserted we'd send only one result!
  }
}
