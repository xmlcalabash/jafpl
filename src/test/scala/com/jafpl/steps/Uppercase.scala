package com.jafpl.steps

import com.jafpl.exceptions.JafplException
import com.jafpl.messages.{ItemMessage, Message, Metadata}

class Uppercase extends DefaultStep {
  override def inputSpec: PortSpecification = PortSpecification.SOURCE
  override def outputSpec: PortSpecification = PortSpecification.RESULT

  override def consume(port: String, message: Message): Unit = {
    message match {
      case item: ItemMessage =>
        item.item match {
          case s: String =>
            consumer.get.consume("result", new ItemMessage(s.toUpperCase(), Metadata.STRING))
          case _ =>
            throw JafplException.unexpectedItemType(item.toString, port, location)
        }
      case _ =>
        throw JafplException.unexpectedMessage(message.toString, port, location)
    }
  }
}
