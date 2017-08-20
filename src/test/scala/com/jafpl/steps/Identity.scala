package com.jafpl.steps

class Identity(allowSeq: Boolean) extends DefaultStep {
  def this() {
    this(true)
  }

  override def inputSpec: PortBindingSpecification = {
    if (allowSeq) {
      PortBindingSpecification.SOURCESEQ
    } else {
      PortBindingSpecification.SOURCE
    }
  }
  override def outputSpec: PortBindingSpecification = {
    if (allowSeq) {
      PortBindingSpecification.RESULTSEQ
    } else {
      PortBindingSpecification.RESULT
    }
  }

  override def receive(port: String, item: Any): Unit = {
    consumer.get.send("result", item)
  }
}
