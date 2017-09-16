package com.jafpl.steps

import com.jafpl.exceptions.PipelineException
import com.jafpl.messages.{ItemMessage, Message, Metadata}
import com.jafpl.util.UniqueId

class Decrement() extends DefaultStep {
  private val _id = UniqueId.nextId.toString
  override def inputSpec: PortSpecification = PortSpecification.SOURCE
  override def outputSpec: PortSpecification = PortSpecification.RESULT

  override def id: String = _id
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
