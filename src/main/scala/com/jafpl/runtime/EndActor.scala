package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{ContainerEnd, Node}
import com.jafpl.messages.Message
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput, GStop}
import com.jafpl.steps.{Manifold, PortSpecification}

import scala.collection.mutable

private[runtime] class EndActor(private val monitor: ActorRef,
                                override protected val runtime: GraphRuntime,
                                override protected val node: ContainerEnd) extends NodeActor(monitor, runtime, node)  {
  protected val unfinishedChildren = mutable.HashSet.empty[Node]
  protected var finished = false
  logEvent = TraceEvent.END

  override protected def initialize(): Unit = {
    trace("INIT", s"$node", logEvent)
    super.initialize()
    reset()
  }

  override protected def reset(): Unit = {
    trace("RESET", s"$node", logEvent)
    unfinishedChildren.clear()
    for (child <- node.start.get.children) {
      unfinishedChildren.add(child)
    }
    readyToRun = true
    finished = false
  }

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node.$fromPort -> $port", logEvent)

    // Container ends are special, they copy input they receive on "X" to the
    // output named "X" on the container start.
    val count = node.start.get.outputCardinalities.getOrElse(port, 0L) + 1
    node.start.get.outputCardinalities.put(port, count)
    monitor ! GOutput(node.start.get, port, item)
  }

  override protected def close(port: String): Unit = {
    trace("CLOSE", s"$node.$port", logEvent)
    try {
      // Are we closing an input or an output?
      val manifold = node.start.get.manifold
      if (manifold.isDefined) {
        if (manifold.get.inputSpec.ports.contains(port)) {
          val count = node.start.get.inputCardinalities.getOrElse(port, 0L)
          manifold.get.inputSpec.checkInputCardinality(port, count)
        } else if (manifold.get.outputSpec.ports.contains(port)) {
          val count = node.start.get.outputCardinalities.getOrElse(port, 0L)
          manifold.get.outputSpec.checkOutputCardinality(port, count)
        }
      }
    } catch {
      case ex: JafplException =>
        monitor ! GException(Some(node), ex)
    }

    openInputs -= port
    monitor ! GClose(node.start.get, port)
    checkFinished()
  }

  override protected def run(): Unit = {
    trace("RUN", s"$node", logEvent)
    log.error(s"run() called on $node", "StepExec")
  }

  protected[runtime] def finished(otherNode: Node): Unit = {
    trace("FINISHED", s"$node $otherNode", logEvent)
    unfinishedChildren -= otherNode
    checkFinished()
  }

  protected[runtime] def checkFinished(): Unit = {
    trace("CHKFINISH", s"${node.start.get}/end ready:$readyToRun inputs:${openInputs.isEmpty} children:${unfinishedChildren.isEmpty} ${!finished}", "StepFinished", logEvent)
    for (child <- unfinishedChildren) {
      trace(s"...UNFINSH", s"$child", logEvent)
    }
    if (readyToRun && !finished) {
      if (openInputs.isEmpty && unfinishedChildren.isEmpty) {
        finished = true
        monitor ! GFinished(node)
      }
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [End]"
  }
}
