package com.jafpl.runtime

import com.jafpl.graph.Node
import com.jafpl.messages.Message
import com.jafpl.steps.DataProvider

class InputProxy(private val node: Node, private val port: String, private val scheduler: Scheduler) extends DataProvider {
  private var _closed = false

  def closed: Boolean = _closed

  override def send(message: Message): Unit = {
    scheduler.receive(node, "result", message)
  }

  def close(): Unit = {
    _closed = true
  }
}
