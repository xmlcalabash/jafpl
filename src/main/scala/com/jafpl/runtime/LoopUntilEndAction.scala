package com.jafpl.runtime

import com.jafpl.graph.{ContainerEnd, LoopUntilStart}
import com.jafpl.messages.Message
import com.jafpl.util.TraceEventManager

class LoopUntilEndAction(override val node: ContainerEnd) extends EndAction(node) {
  private var startAction: LoopUntilAction = _
  private val loopStart: LoopUntilStart = node.start.get.asInstanceOf[LoopUntilStart]

  def loopStartAction: LoopUntilAction = startAction
  def loopStartAction_=(start: LoopUntilAction): Unit = {
    startAction = start
  }

  override def receive(port: String, message: Message): Unit = {
    if (port == "test") {
      // This is magic, it needs to go back to the loop start
      startAction.receive("test", message)
    } else {
      if (loopStart.returnAll) {
        tracer.trace(s"SENDING ${port}", TraceEventManager.UNTIL)
        super.receive(port, message)
      } else {
        startAction.receive("lastItem", message)
      }
    }
  }
}
