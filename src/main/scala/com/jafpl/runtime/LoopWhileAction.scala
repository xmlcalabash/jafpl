package com.jafpl.runtime

import com.jafpl.exceptions.JafplException
import com.jafpl.graph.LoopWhileStart
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.NodeState.NodeState

class LoopWhileAction(override val node: LoopWhileStart) extends LoopAction(node) {
  private var currentItem = Option.empty[ItemMessage]
  private var looping = false
  private var firstItem = true
  private var inputPort = Option.empty[String]
  private var endAction: LoopWhileEndAction = _

  def loopEndAction: LoopWhileEndAction = endAction
  def loopEndAction_=(end: LoopWhileEndAction): Unit = {
    endAction = end
  }

  override def receive(port: String, message: Message): Unit = {
    message match {
      case item: ItemMessage =>
        if (port == "test") {
          currentItem = Some(item)
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

  override def finished(): Boolean = node.done

  override def run(): Unit = {
    super.run()

    node.done = currentItem.isEmpty || !node.tester.test(List(currentItem.get), receivedBindings.toMap)

    if (!looping) {
      looping = true
      node.iterationPosition = 0
      node.iterationSize = 0
    }

    if (node.done) {
      skipChildren()
    } else {
      node.iterationPosition += 1
      node.iterationSize += 1
      scheduler.receive(node, "current", currentItem.get)
      startChildren()
    }

    scheduler.finish(node)
  }

  override def reset(state: NodeState): Unit = {
    super.reset(state)

    node.iterationPosition = 0
    node.iterationSize = 0
    currentItem = None
    looping = false
    firstItem = true
    node.done = false
  }
}
