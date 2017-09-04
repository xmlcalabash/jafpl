package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{ContainerEnd, Node}
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput, GStop}

import scala.collection.mutable

private[runtime] class EndActor(private val monitor: ActorRef,
                              private val runtime: GraphRuntime,
                              private val node: ContainerEnd) extends NodeActor(monitor, runtime, node)  {
  protected val unfinishedChildren = mutable.HashSet.empty[Node]

  override protected def initialize(): Unit = {
    super.initialize()
    reset()
  }

  override protected def reset(): Unit = {
    unfinishedChildren.clear()
    for (child <- node.start.get.children) {
      unfinishedChildren.add(child)
    }
    readyToRun = true
  }

  override protected def input(port: String, item: Message): Unit = {
    // Container ends are special, they copy input they receive on "X" to the
    // output named "X" on the container start.
    monitor ! GOutput(node.start.get, port, item)
  }

  override protected def close(port: String): Unit = {
    openInputs -= port
    monitor ! GClose(node.start.get, port)
    checkFinished()
  }

  override protected def run(): Unit = {
    log.error(s"run() called on $node", "StepExec")
  }

  protected[runtime] def finished(otherNode: Node): Unit = {
    trace(s"CHILDFIN $otherNode", "StepFinished")
    unfinishedChildren -= otherNode
    checkFinished()
  }

  protected[runtime] def checkFinished(): Unit = {
    trace(s"FINIFRDY ${node.start.get}/end ready:$readyToRun inputs:${openInputs.isEmpty} children:${unfinishedChildren.isEmpty}", "StepFinished")
    for (child <- unfinishedChildren) {
      trace(s"........ $child", "StepFinished")
    }
    if (readyToRun) {
      if (openInputs.isEmpty && unfinishedChildren.isEmpty) {
        monitor ! GFinished(node)
      }
    }
  }
}
