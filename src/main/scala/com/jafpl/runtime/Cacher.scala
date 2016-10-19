package com.jafpl.runtime

import com.jafpl.items.GenericItem
import com.jafpl.messages.ItemMessage

import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/19/16.
  */
class Cacher extends DefaultStep {
  val cache = ListBuffer.empty[GenericItem]

  override def run(): Unit = {
    println("Cacher runs")
    for (item <- cache) {
      controller.send("result", item)
    }
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    if (port == "source") {
      cache += msg.item
    } else {
      logger.debug(s"Input from unexpected port: '$port' on Cacher")
    }
  }
}
