package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{ContainerEnd, Node}
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class ConditionalEndActor(private val monitor: ActorRef,
                                         private val runtime: GraphRuntime,
                                         private val node: ContainerEnd) extends EndActor(monitor, runtime, node)  {
  val buffer = mutable.HashMap.empty[String, ListBuffer[Message]]

  override protected def reset(): Unit = {
    super.reset()
    buffer.clear()
  }

  override protected def input(port: String, item: Message): Unit = {
    // Buffer everything in case it all goes bang
    if (!buffer.contains(port)) {
      buffer.put(port, ListBuffer.empty[Message])
    }
    buffer(port) += item
  }

  override protected def close(port: String): Unit = {
    openInputs -= port
    trace(s"check finish for close $port", "xxx")
    checkFinished()
  }

  override protected[runtime] def finished(otherNode: Node): Unit = {
    trace(s"END FINISHED $node / $otherNode", "StepFinished")
    unfinishedChildren -= otherNode
    trace(s"check finish for finished $otherNode", "xxx")
    checkFinished()
  }

  override protected[runtime] def checkFinished(): Unit = {
    trace(s"FNIFR $node (${node.start.getOrElse("!START")}) $readyToRun ${openInputs.isEmpty}: ${unfinishedChildren.isEmpty}", "StepFinished")
    for (child <- unfinishedChildren) {
      trace(s"!FNSH ...$child", "StepFinished")
    }
    if (readyToRun) {
      if (openInputs.isEmpty && unfinishedChildren.isEmpty) {
        for (port <- buffer.keySet) {
          for (item <- buffer(port)) {
            trace(s"Conditional end actor ${node.start} sends to $port: $item", "StepIO")
            monitor ! GOutput(node.start.get, port, item)
          }
        }
        for (input <- node.inputs) {
          monitor ! GClose(node.start.get, input)
        }
        trace(s"FINSH $node", "StepFinished")
        monitor ! GFinished(node)
      }
    }
  }
}
