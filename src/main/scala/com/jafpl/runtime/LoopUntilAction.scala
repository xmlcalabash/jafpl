package com.jafpl.runtime

import com.jafpl.exceptions.JafplException
import com.jafpl.graph.LoopUntilStart
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.NodeState.NodeState
import com.jafpl.util.TraceEventManager

class LoopUntilAction(override val node: LoopUntilStart) extends LoopAction(node) {
  private var currentItem = Option.empty[ItemMessage]
  private var nextItem = Option.empty[ItemMessage]
  private var looping = false
  private var endAction: LoopUntilEndAction = _
  private var inputPort = Option.empty[String]

  def loopEndAction: LoopUntilEndAction = endAction
  def loopEndAction_=(end: LoopUntilEndAction): Unit = {
    endAction = end
  }

  override def receive(port: String, message: Message): Unit = {
    super.receive(port, message)
    message match {
      case item: ItemMessage =>
        if (port == "test") {
          if (currentItem.isEmpty) {
            currentItem = Some(item)
          }
          nextItem = Some(item)
        } else {
          if (inputPort.isEmpty || inputPort.get == port) {
            if (currentItem.isDefined) {
              throw JafplException.unexpectedSequence(node.userLabel.getOrElse("cx:until"), port, node.location)
            }
            currentItem = Some(item)
            inputPort = Some(port)
          } else {
            throw JafplException.invalidUntilPort(port, inputPort.get, node.location)
          }
        }
      case _ =>
        throw JafplException.unexpectedMessage(message.toString, port, node.location)
    }
  }

  override def finished(): Boolean = {
    val theSame = node.comparator.areTheSame(currentItem.get.item, nextItem.get.item)
    tracer.trace(s"UNTIL ${currentItem.get} == ${nextItem.get}: ${theSame}", TraceEventManager.UNTIL)
    if (theSame) {
      endAction.sendResults()
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
    looping = false
  }
}
