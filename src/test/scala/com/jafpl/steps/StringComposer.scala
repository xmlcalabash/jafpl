package com.jafpl.steps

import com.jafpl.exceptions.{PipelineException, StepException}

import scala.collection.mutable.ListBuffer

class StringComposer extends ViewportComposer {
  val items = ListBuffer.empty[StringViewportItem]
  var suffix = ""

  override def decompose(item: Any): List[ViewportItem] = {
    item match {
      case stringItem: String =>
        var s = stringItem
        val nextWord = "(\\W*)(\\w+)(.*)".r
        var more = true
        while (more) {
          s match {
            case nextWord(prefix,word,rest) =>
              items += new StringViewportItem(prefix, word)
              s = rest
            case _ =>
              suffix = s
              more = false
          }
        }

      case _ => throw new StepException("UnexpectedType", s"Unexpected item type: $item")
    }

    items.toList
  }

  override def recompose(): Any = {
    var wholeItem = ""
    for (item <- items) {
      wholeItem += item.prefix
      for (s <- item.transformedItems) {
        wholeItem += s
      }
    }
    wholeItem += suffix
    wholeItem
  }
}
