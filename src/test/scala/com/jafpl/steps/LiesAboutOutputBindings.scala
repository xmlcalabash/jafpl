package com.jafpl.steps

import com.jafpl.messages.{ItemMessage, Metadata}

class LiesAboutOutputBindings extends DefaultStep {
  override def inputSpec: PortSpecification = PortSpecification.SOURCESEQ
  override def outputSpec: PortSpecification = PortSpecification.RESULT

  override def run(): Unit = {
    consumer.get.consume("result", new ItemMessage("one", Metadata.STRING))
    // But we asserted we'd send only one result!
    consumer.get.consume("result", new ItemMessage("two", Metadata.STRING))
  }
}
