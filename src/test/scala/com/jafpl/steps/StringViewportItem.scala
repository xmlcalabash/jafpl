package com.jafpl.steps

import com.jafpl.exceptions.PipelineException
import com.jafpl.messages.Metadata

import scala.collection.mutable.ListBuffer

class StringViewportItem(val prefix: String, val item: String) extends ViewportItem {
  private var items = ListBuffer.empty[String]

  def transformedItems: List[String] = items.toList

  override def getItem: Any = item

  override def getMetadata: Metadata = Metadata.STRING

  override def putItems(xformed: List[Any]): Unit = {
    for (item <- xformed) {
      item match {
        case s: String => items += s
        case _ => throw new PipelineException("UnexpectedType", s"Unexpected item type: $item", None)
      }
    }
  }
}
