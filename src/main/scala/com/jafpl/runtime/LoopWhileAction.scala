package com.jafpl.runtime

import com.jafpl.exceptions.JafplException
import com.jafpl.graph.LoopWhileStart
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.NodeState.NodeState

class LoopWhileAction(override val node: LoopWhileStart) extends LoopAction(node) {
  private var currentItem = Option.empty[ItemMessage]
  private var looping = false
  private var firstItem = true
  private var _done = false

  def done: Boolean = _done

  override def receive(port: String, message: Message): Unit = {
    message match {
      case item: ItemMessage =>
        port match {
          case "source" =>
            if (currentItem.isDefined) {
              throw new RuntimeException("Sequence on loop while source")
            }
            currentItem = Some(item)
          case "test" =>
            currentItem = Some(item)
          case _ =>
            throw new RuntimeException(s"Unexpected input port on until: ${port}")
        }
      case _ =>
        throw JafplException.unexpectedMessage(message.toString, port, node.location)
    }
  }

  override def finished(): Boolean = _done

  override def run(): Unit = {
    super.run()

    _done = currentItem.isEmpty || !node.tester.test(List(currentItem.get), receivedBindings.toMap)

    if (!looping) {
      looping = true
      node.iterationPosition = 0
      node.iterationSize = 0
    }

    if (_done) {
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
    _done = false
  }
}
