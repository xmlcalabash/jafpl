package com.jafpl.runtime

import com.jafpl.graph.Node
import com.jafpl.messages.Message
import com.jafpl.steps.DataProvider

class OptionProxy(private val node: Node, private val name: String, private val scheduler: Scheduler) {
  def setValue(message: Message): Unit = {
    scheduler.receiveBinding(node, message)

  }
}
