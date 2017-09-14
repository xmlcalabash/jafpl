package com.jafpl.steps

import com.jafpl.exceptions.PipelineException
import com.jafpl.messages.{ItemMessage, Message, Metadata}

class Uppercase extends DefaultStep {
  override def inputSpec: PortSpecification = PortSpecification.SOURCE
  override def outputSpec: PortSpecification = PortSpecification.RESULT

  override def receive(port: String, message: Message): Unit = {
    message match {
      case item: ItemMessage =>
        item.item match {
          case s: String =>
            consumer.get.receive("result", new ItemMessage(s.toUpperCase(), Metadata.STRING))
          case _ =>
            throw new PipelineException("unexpectedtype", "Unexpected input type", location)
        }
      case _ =>
        throw new PipelineException("unexpectedmsg", "Unexpected message type", location)
    }
  }
}
