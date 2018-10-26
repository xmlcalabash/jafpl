package com.jafpl.steps

import com.jafpl.messages.{ItemMessage, Message, Metadata}

class ExceptionTranslator() extends DefaultStep {
  override def inputSpec: PortSpecification = {
    PortSpecification.SOURCESEQ
  }
  override def outputSpec: PortSpecification = {
    PortSpecification.RESULTSEQ
  }

  override def receive(port: String, message: Message): Unit = {
    consumer.get.receive("result", new ItemMessage("Caught one!", Metadata.STRING))
  }
}
