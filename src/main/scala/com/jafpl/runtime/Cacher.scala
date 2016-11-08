package com.jafpl.runtime

import com.jafpl.items.GenericItem
import com.jafpl.messages.ItemMessage

import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/19/16.
  */
class Cacher extends DefaultStep {
  val cache = ListBuffer.empty[GenericItem]
  var ready = true

  override def run(): Unit = {
    if (true || ready) {
      logger.debug("Sending {} items from cache: {}", cache.size, this)
      for (item <- cache) {
        controller.send("result", item)
      }
      ready = false
    } else {
      logger.debug("Attempt to run cache without reset: {}", this)
    }
  }

  override def reset(): Unit = {
    logger.debug("Reset {}: {}", ready, this)
    ready = true
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    if (port == "source") {
      cache += msg.item
    } else {
      logger.debug(s"Input from unexpected port: '$port' on Cacher")
    }
  }
}
