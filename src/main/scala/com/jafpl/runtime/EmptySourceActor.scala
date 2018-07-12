package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{EmptySource, Node}
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished}

private[runtime] class EmptySourceActor(private val monitor: ActorRef,
                                        private val runtime: GraphRuntime,
                                        private val node: EmptySource)
  extends NodeActor(monitor, runtime, node)  {

  var hasBeenReset = false

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    throw JafplException.inputOnEmptySource(from.toString, fromPort, port, item.toString, node.location)
  }

  override protected def reset(): Unit = {
    readyToRun = true
    hasBeenReset = true
    openInputs.clear()
  }

  override protected def run(): Unit = {
    trace(s"RUNESRC↴ $node", "StepIO")
    monitor ! GClose(node, "result")
    monitor ! GFinished(node)
  }
}
