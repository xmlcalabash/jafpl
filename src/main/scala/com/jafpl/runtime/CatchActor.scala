package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.CatchStart
import com.jafpl.messages.ExceptionMessage

private[runtime] class CatchActor(private val monitor: ActorRef,
                                   override protected val runtime: GraphRuntime,
                                   override protected val node: CatchStart) extends StartActor(monitor, runtime, node) {

  override protected def reset(): Unit = {
    node.cause = None
    super.reset()
  }

  override protected def run(): Unit = {
    if (openOutputs.contains("error")) {
      if (node.cause.isDefined) {
        sendMessage("error", new ExceptionMessage(node.cause.get))
      }
      sendClose("error")
    }
    super.run()
  }
}
