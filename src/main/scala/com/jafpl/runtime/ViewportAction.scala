package com.jafpl.runtime

import com.jafpl.exceptions.JafplException
import com.jafpl.graph.ViewportStart
import com.jafpl.messages.{ItemMessage, Message, PipelineMessage}
import com.jafpl.runtime.AbstractAction.showRunningMessage
import com.jafpl.runtime.NodeState.NodeState
import com.jafpl.steps.ViewportItem

import scala.collection.mutable.ListBuffer

class ViewportAction(override val node: ViewportStart) extends LoopAction(node) {
  private var _index = 0
  private var sourceItem = Option.empty[ItemMessage]

  val itemQueue: ListBuffer[ViewportItem] = ListBuffer.empty[ViewportItem]
  def index: Int = _index

  override def receive(port: String, message: Message): Unit = {
    super.receive(port, message)
    if (port.startsWith("#depends") || port == "#bindings") {
      scheduler.receive(node, port, message)
    } else {
      message match {
        case item: ItemMessage =>
          sourceItem = Some(item)
        case _ =>
          throw JafplException.unexpectedMessage(message.toString, port, node.location)
      }
    }
  }

  // We have to be able to distinguish between we never received any input
  // and we consumed all of our input.
  def receivedInput: Boolean = sourceItem.isDefined

  override def run(): Unit = {
    super.run()
    if (showRunningMessage) {
      logger.info("Running viewport")
    }

    if (node.iterationPosition == 0) {
      if (sourceItem.isDefined) {
        try {
          node.composer.runtimeBindings(receivedBindings.toMap)
          for (item <- node.composer.decompose(sourceItem.get)) {
            itemQueue += item
          }
        } catch {
          case t: Throwable =>
            scheduler.reportException(node, t)
            return
        }
      }
      _index = 0
      node.iterationSize = itemQueue.size
    } else {
      _index += 1
    }

    if (itemQueue.nonEmpty) {
      node.iterationPosition += 1
      val item = itemQueue(_index)
      scheduler.receive(node, "current", new PipelineMessage(item.getItem, item.getMetadata))
      startChildren()
    } else {
      // There was no input, don't try to run the children...
      skipChildren()
    }

    scheduler.finish(node)
  }

  override def reset(state: NodeState): Unit = {
    super.reset(state)

    node.iterationSize = 0L
    node.iterationPosition = 0L
    itemQueue.clear()
    _index = 0
    sourceItem = None
  }

}
