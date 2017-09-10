package com.jafpl.steps

import com.jafpl.exceptions.StepException
import com.jafpl.messages.{ItemMessage, Message, Metadata}

import scala.collection.mutable.ListBuffer

class StringComposer extends ViewportComposer {
  private var metadata: Metadata = Metadata.BLANK
  private val items = ListBuffer.empty[StringViewportItem]
  private var suffix = ""

  override def decompose(message: Message): List[ViewportItem] = {
    message match {
      case imsg: ItemMessage =>
        imsg.item match {
          case stringItem: String =>
            this.metadata = metadata
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
          case _ => throw new StepException("UnexpectedType", s"Unexpected item type: $imsg.item")
        }
      case _ => throw new StepException("UnexpectedMsg", s"Unexpected message type: $message")
    }

    items.toList
  }

  override def recompose(): ItemMessage = {
    var wholeItem = ""
    for (item <- items) {
      wholeItem += item.prefix
      for (s <- item.transformedItems) {
        wholeItem += s
      }
    }
    wholeItem += suffix
    new ItemMessage(wholeItem, metadata)
  }
}
