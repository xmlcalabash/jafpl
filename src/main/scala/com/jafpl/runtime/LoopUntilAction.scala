package com.jafpl.runtime

import com.jafpl.exceptions.JafplException
import com.jafpl.graph.LoopUntilStart
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.NodeState.NodeState
import com.jafpl.util.TraceEventManager

class LoopUntilAction(override val node: LoopUntilStart) extends LoopAction(node) {
  private var currentItem = Option.empty[ItemMessage]
  private var nextItem = Option.empty[ItemMessage]
  private var lastItem = Option.empty[Message]
  private var looping = false

  override def receive(port: String, message: Message): Unit = {
    if (port == "lastItem") {
      // This is just straight-up magic
      lastItem = Some(message)
      return
    }

    super.receive(port, message)
    message match {
      case item: ItemMessage =>
        port match {
          case "source" =>
            if (currentItem.isDefined) {
              throw new RuntimeException("Sequence on loop until source")
            }
            currentItem = Some(item)
          case "test" =>
            if (currentItem.isEmpty) {
              currentItem = Some(item)
            }
            nextItem = Some(item)
          case _ =>
            throw new RuntimeException(s"Unexpected input port on until: ${port}")
        }
      case _ =>
        throw JafplException.unexpectedMessage(message.toString, port, node.location)
    }
  }

  override def finished(): Boolean = {
    val theSame = node.comparator.areTheSame(currentItem.get.item, nextItem.get.item)
    tracer.trace(s"UNTIL ${currentItem.get} == ${nextItem.get}: ${theSame}", TraceEventManager.UNTIL)
    if (theSame) {
      if (lastItem.isDefined) {
        scheduler.receive(node.containerEnd, "result", lastItem.get)
      }
    } else {
      currentItem = nextItem
    }
    theSame
  }

  override def run(): Unit = {
    super.run()

    if (!looping) {
      looping = true
      node.iterationPosition = 0
      node.iterationSize = 0
    }

    node.iterationPosition += 1
    node.iterationSize += 1
    scheduler.receive(node, "current", currentItem.get)

    startChildren()
    scheduler.finish(node)
  }

  override def reset(state: NodeState): Unit = {
    super.reset(state)

    node.iterationPosition = 0
    node.iterationSize = 0
    currentItem = None
    nextItem = None
    lastItem = None
    looping = false
  }
}
