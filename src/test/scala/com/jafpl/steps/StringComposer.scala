package com.jafpl.steps

import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.Location
import com.jafpl.messages.{ItemMessage, Message, Metadata}

import scala.collection.mutable.ListBuffer

class StringComposer(location: Option[Location]) extends ViewportComposer {
  private var metadata: Metadata = Metadata.BLANK
  private val items = ListBuffer.empty[StringViewportItem]
  private var suffix = ""

  def this() {
    this(None)
  }

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
          case _ => throw new PipelineException("UnexpectedType", s"Unexpected item type: $imsg.item", location)
        }
      case _ => throw new PipelineException("UnexpectedMsg", s"Unexpected message type: $message", location)
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
