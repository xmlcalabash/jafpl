package com.jafpl.steps

import com.jafpl.messages.Metadata

class Identity(allowSeq: Boolean) extends DefaultStep {
  def this() {
    this(true)
  }

  override def inputSpec: PortSpecification = {
    if (allowSeq) {
      PortSpecification.SOURCESEQ
    } else {
      PortSpecification.SOURCE
    }
  }
  override def outputSpec: PortSpecification = {
    if (allowSeq) {
      PortSpecification.RESULTSEQ
    } else {
      PortSpecification.RESULT
    }
  }

  override def receive(port: String, item: Any, metadata: Metadata): Unit = {
    consumer.get.send("result", item, metadata)
  }
}
