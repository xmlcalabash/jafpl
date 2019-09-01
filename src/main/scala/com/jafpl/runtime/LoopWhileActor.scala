package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{LoopWhileStart, Node, NodeState}
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.NodeActor.{NAbort, NFinished, NReset, NRun}

private[runtime] class LoopWhileActor(private val monitor: ActorRef,
                                       override protected val runtime: GraphRuntime,
                                       override protected val node: LoopWhileStart)
  extends LoopActor(monitor, runtime, node) {

  private var currentItem = Option.empty[ItemMessage]
  private var lastItem = Option.empty[Message]
  logEvent = TraceEvent.LOOPWHILE
  node.iterationPosition = 0L
  node.iterationSize = 0L

  override protected def configurePorts(): Unit = {
    super.configurePorts()
    openInputs -= "test"
  }

  override protected def input(port: String, message: Message): Unit = {
    if (port == "source" || port == "test") {
      message match {
        case item: ItemMessage =>
          currentItem = Some(item)
        case _ =>
          throw JafplException.unexpectedMessage(message.toString, port, node.location)
      }
    } else {
      if (openOutputs.contains(port)) {
        if (node.returnAll) {
          super.input(port, message)
        } else {
          // FIXME: what about multiple output ports?
          lastItem = Some(message)
        }
      }
    }
  }

  override protected def run(): Unit = {
    val pass = currentItem.isDefined && node.tester.test(List(currentItem.get), receivedBindings.toMap)
    if (pass) {
      stateChange(node, NodeState.RUNNING)
      node.iterationPosition += 1
      sendMessage("current", currentItem.get)
      sendClose("current")
      for (cnode <- node.children) {
        if (cnode.state == NodeState.READY) {
          stateChange(cnode, NodeState.RUNNING)
          actors(cnode) ! NRun()
        }
      }
    } else {
      // If we're not going to run at all, abort the children so that
      // any input or output cardinality errors don't raise exceptions
      for (cnode <- node.children) {
        stateChange(cnode, NodeState.ABORTING)
        actors(cnode) ! NAbort()
      }
      closeOutputs()
      parent ! NFinished(node)
    }
  }

  override protected def closeOutputs(): Unit = {
    for (port <- openOutputs) {
      if (port != "test") {
        sendClose(port)
      }
      openOutputs -= port
    }
  }

  override protected def finished(child: Node): Unit = {
    stateChange(child, NodeState.FINISHED)
    var finished = true
    for (cnode <- node.children) {
      finished = finished && cnode.state == NodeState.FINISHED
    }
    if (finished) {
      val loopAgain = node.tester.test(List(currentItem.get), receivedBindings.toMap)
      if (loopAgain) {
        stateChange(node, NodeState.LOOPING)
        for (child <- node.children) {
          actors(child) ! NReset()
        }
      } else {
        if (lastItem.isDefined) {
          for (port <- openOutputs) {
            sendMessage(port, lastItem.get)
          }
        }
        closeOutputs()
        parent ! NFinished(node)
      }
    } else {
      trace("UNFINISH", s"${nodeState(node)}", TraceEvent.STATECHANGE)
    }
  }
}
