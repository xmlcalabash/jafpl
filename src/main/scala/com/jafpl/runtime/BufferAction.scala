package com.jafpl.runtime

import com.jafpl.graph.Buffer
import com.jafpl.messages.{BindingMessage, Message}
import com.jafpl.runtime.NodeState.NodeState
import com.jafpl.util.TraceEventManager

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class BufferAction(override val node: Buffer) extends AbstractAction(node) {
  // Buffer is special; it must not consume its inputs each time its run because the whole point
  // is that it buffers them. Since reading a port consumes the messages, we have our own buffer.
  private val buffer = mutable.HashMap.empty[String, ListBuffer[Message]]

  override def receive(port: String, message: Message): Unit = {
    // Buffer all messages, even bindings
    tracer.trace(s"RECV  $this for $port: $message", TraceEventManager.RECEIVE)
    if (buffer.contains(port)) {
      buffer(port) += message
    } else {
      buffer.put(port, ListBuffer(message))
    }
  }

  override def run(): Unit = {
    super.run()
    for (port <- buffer.keys) {
      for (message <- buffer(port)) {
        scheduler.receive(this, "result", message)
      }
    }
    scheduler.finish(node)
  }

  override def reset(state: NodeState): Unit = {
    super.reset(state)
    buffer.clear()
  }

  override def stop(): Unit = {
    super.stop()
    buffer.clear()
  }
  override def abort(): Unit = {
    super.stop()
    buffer.clear()
  }
}
