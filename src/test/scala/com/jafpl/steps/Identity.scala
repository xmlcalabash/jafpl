package com.jafpl.steps

import com.jafpl.items.GenericItem
import com.jafpl.messages.ItemMessage
import com.jafpl.runtime.DefaultStep

import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/7/16.
  */
class Identity() extends DefaultStep {
  val items = ListBuffer.empty[GenericItem]
  label = "Identity"

  override def run(): Unit = {
    for (item <- items) {
      controller.send("result", item)
    }
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    items += msg.item
  }
}
