package com.jafpl.runtime

import com.jafpl.messages.ItemMessage

/**
  * Created by ndw on 10/10/16.
  */
class Grouper extends DefaultCompoundStep {
  label = "_grouper"

  final def runAgain = false

  override def receive(port: String, msg: ItemMessage): Unit = {
    logger.warn("Received document on Grouper: this should never happen")
  }
}
