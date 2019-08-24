package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{LoopUntilStart, Node, NodeState}
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.NodeActor.{NFinished, NReset, NRunIfReady}

private[runtime] class LoopUntilActor(private val monitor: ActorRef,
                                       override protected val runtime: GraphRuntime,
                                       override protected val node: LoopUntilStart)
  extends LoopActor(monitor, runtime, node) {

  private var currentItem = Option.empty[ItemMessage]
  private var nextItem = Option.empty[ItemMessage]
  private var lastItem = Option.empty[Message]
  logEvent = TraceEvent.LOOPUNTIL
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
          if (currentItem.isEmpty) {
            currentItem = Some(item)
          } else {
            nextItem = Some(item)
          }
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

  override protected def reset(): Unit = {
    node.iterationPosition = 0L
    node.iterationSize = 0L
    super.reset()
  }

  override protected def run(): Unit = {
    stateChange(node, NodeState.RUNNING)
    node.iterationPosition += 1
    sendMessage("current", currentItem.get)
    sendClose("current")
    for (cnode <- node.children) {
      actors(cnode) ! NRunIfReady()
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
      val theSame = node.comparator.areTheSame(currentItem.get.item, nextItem.get.item)
      if (theSame) {
        trace("UNTILC", s"${currentItem.get} == ${nextItem.get}", TraceEvent.NMESSAGES)
        if (lastItem.isDefined) {
          for (port <- openOutputs) {
            sendMessage(port, lastItem.get)
          }
        }
        closeOutputs()
        parent ! NFinished(node)
      } else {
        trace("UNTILC", s"${currentItem.get} != ${nextItem.get}", TraceEvent.NMESSAGES)
        stateChange(node, NodeState.LOOPING)
        currentItem = nextItem
        for (child <- node.children) {
          actors(child) ! NReset()
        }
      }
    } else {
      trace("UNFINISH", s"${nodeState(node)}", TraceEvent.STATECHANGE)
    }
  }
}
