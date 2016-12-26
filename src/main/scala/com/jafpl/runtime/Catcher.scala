package com.jafpl.runtime

import com.jafpl.messages.ItemMessage

/**
  * Created by ndw on 10/10/16.
  */
class Catcher extends DefaultCompoundStep {
  label = "_catcher"

  override def runAgain: Boolean = false

  override def receive(port: String, msg: ItemMessage): Unit = {
    logger.warn("Received document on Catcher: this should never happen")
  }
}

