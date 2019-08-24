package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{JoinMode, Joiner, NodeState}
import com.jafpl.messages.Message
import com.jafpl.runtime.NodeActor.NFinished

private[runtime] class JoinerActor(private val monitor: ActorRef,
                                   override protected val runtime: GraphRuntime,
                                   override protected val node: Joiner) extends AtomicActor(monitor, runtime, node) {
  private val joinBuffer = new IOBuffer()
  logEvent = TraceEvent.JOINER

  override protected def input(port: String, item: Message): Unit = {
    joinBuffer.consume(port, item)
  }

  override protected def reset(): Unit = {
    joinBuffer.reset()
    super.reset()
  }

  override protected def run(): Unit = {
    stateChange(node, NodeState.RUNNING)

    if (node.mode == JoinMode.PRIORITY && joinBuffer.ports.contains("source_1")) {
      for (item <- joinBuffer.messages("source_1")) {
        sendMessage("result", item)
      }
    } else {
      var portNum = 0
      while (joinBuffer.ports.nonEmpty) {
        portNum += 1
        val port = s"source_$portNum"
        if (joinBuffer.ports.contains(port)) {
          for (item <- joinBuffer.messages(port)) {
            sendMessage("result", item)
          }
          joinBuffer.clear(port)
        }
      }
    }
    closeOutputs()
    parent ! NFinished(node)
  }
}
