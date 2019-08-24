package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.Buffer
import com.jafpl.messages.Message
import com.jafpl.runtime.NodeActor.NResetted

import scala.collection.mutable.ListBuffer

private[runtime] class BufferActor(private val monitor: ActorRef,
                                   override protected val runtime: GraphRuntime,
                                   override protected val node: Buffer) extends AtomicActor(monitor, runtime, node) {
  private val buffer = ListBuffer.empty[Message]
  logEvent = TraceEvent.BUFFER

  override protected def reset(): Unit = {
    inputBuffer.reset()
    outputBuffer.reset()
    receivedBindings.clear()
    configurePorts()
    openInputs.clear() // buffered
    parent ! NResetted(node)
  }

  override protected def input(port: String, item: Message): Unit = {
    buffer += item
  }

  override protected def run(): Unit = {
    for (item <- buffer) {
      sendMessage("result", item)
    }
    super.run()
  }
}
