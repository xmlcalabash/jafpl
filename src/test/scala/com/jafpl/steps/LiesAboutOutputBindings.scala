package com.jafpl.steps

class LiesAboutOutputBindings extends DefaultStep {
  override def inputSpec = PortBindingSpecification.SOURCESEQ
  override def outputSpec = PortBindingSpecification.RESULT

  override def run(): Unit = {
    consumer.get.send("result", "one")
    consumer.get.send("result", "two") // but we asserted we'd send only one result!
  }
}
