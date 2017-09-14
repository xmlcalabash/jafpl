package com.jafpl.steps

import com.jafpl.exceptions.PipelineException
import com.jafpl.messages.{ItemMessage, Message, Metadata}

class Decrement() extends DefaultStep {
  override def inputSpec: PortSpecification = PortSpecification.SOURCE
  override def outputSpec: PortSpecification = PortSpecification.RESULT

  override def receive(port: String, message: Message): Unit = {
    message match {
      case item: ItemMessage =>
        item.item match {
          case num: Long => consumer.get.receive("result", new ItemMessage(num - 1, Metadata.NUMBER))
          case num: Int => consumer.get.receive("result", new ItemMessage(num - 1, Metadata.NUMBER))
          case _ => throw new PipelineException("nan", "Decrement input not a number: " + item, location)
        }
      case _ => throw new PipelineException("noitems", "No items in message: " + message, location)
    }
  }
}
