package com.jafpl.steps

import com.jafpl.exceptions.PipelineException

import scala.collection.mutable.ListBuffer

class StringViewportItem(val prefix: String, val item: String) extends ViewportItem {
  private var items = ListBuffer.empty[String]

  def transformedItems = items.toList

  override def getItem: Any = item

  override def putItems(xformed: List[Any]): Unit = {
    for (item <- xformed) {
      item match {
        case s: String => items += s
        case _ => throw new PipelineException("UnexpectedType", s"Unexpected item type: $item")
      }
    }
  }
}
