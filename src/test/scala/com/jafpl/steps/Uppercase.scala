package com.jafpl.steps

import com.jafpl.exceptions.StepException
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
            throw new StepException("unexpectedtype", "Unexpected input type")
        }
      case _ =>
        throw new StepException("unexpectedmsg", "Unexpected message type")
    }
  }
}
