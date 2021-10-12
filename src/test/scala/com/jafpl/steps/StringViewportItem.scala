package com.jafpl.steps

import com.jafpl.exceptions.JafplException
import com.jafpl.messages.{ItemMessage, Message, Metadata}

import scala.collection.mutable.ListBuffer

class StringViewportItem(val prefix: String, val item: String) extends ViewportItem {
  private var items = ListBuffer.empty[String]

  def transformedItems: List[String] = items.toList

  override def getItem: Any = item

  override def getMetadata: Metadata = Metadata.STRING

  override def putItems(xformed: List[Message]): Unit = {
    for (item <- xformed) {
      item match {
        case msg: ItemMessage =>
          items += msg.item.toString
        case _ => throw JafplException.unexpectedItemType(item.toString, "source", None)
      }
    }
  }
}
