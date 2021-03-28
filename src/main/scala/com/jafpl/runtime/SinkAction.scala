package com.jafpl.runtime

import com.jafpl.graph.Sink
import com.jafpl.messages.{BindingMessage, Message}
import com.jafpl.util.TraceEventManager

import scala.collection.mutable.ListBuffer

class SinkAction(override val node: Sink) extends AbstractAction(node) {
  override def receive(port: String, message: Message): Unit = {
    message match {
      case msg: BindingMessage =>
        tracer.trace(s"RECVB $this for ${msg.name}", TraceEventManager.BINDING)
      case _ =>
        tracer.trace(s"RECV  $this for $port: $message", TraceEventManager.RECEIVE)
        if (!node.inputs.contains(port) && !port.startsWith("#")) {
          tracer.trace("error", s"INTERNAL ERROR: Ignoring input for unexpected port $this: $port ($message)", TraceEventManager.MESSAGES)
        }
    }
  }

  override def run(): Unit = {
    super.run()
    scheduler.finish(node)
    cleanup()
  }
}
