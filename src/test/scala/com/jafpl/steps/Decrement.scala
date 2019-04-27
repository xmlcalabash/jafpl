package com.jafpl.steps

import com.jafpl.exceptions.JafplException
import com.jafpl.messages.{ItemMessage, Message, Metadata}
import com.jafpl.util.UniqueId

class Decrement() extends DefaultStep {
  override def inputSpec: PortSpecification = PortSpecification.SOURCE
  override def outputSpec: PortSpecification = PortSpecification.RESULT

  override def receive(port: String, message: Message): Unit = {
    message match {
      case item: ItemMessage =>
        item.item match {
          case num: Long => consumer.get.receive("result", new ItemMessage(num - 1, Metadata.NUMBER))
          case num: Int => consumer.get.receive("result", new ItemMessage(num - 1, Metadata.NUMBER))
          case _ => JafplException.internalError(s"Decrement input is not a number: $item", location)
        }
      case _ => JafplException.internalError(s"No items in message: $message", location)
    }
  }
}
