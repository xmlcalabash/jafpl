package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{Node, NodeState, ViewportStart}
import com.jafpl.messages.{ItemMessage, Message, PipelineMessage}
import com.jafpl.runtime.NodeActor.{NFinished, NReset, NRunIfReady}
import com.jafpl.steps.ViewportItem

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class ViewportActor(private val monitor: ActorRef,
                                      override protected val runtime: GraphRuntime,
                                      override protected val node: ViewportStart)
  extends LoopActor(monitor, runtime, node) {

  private val itemQueue = ListBuffer.empty[ViewportItem]
  private var index = 0
  private var sourceItem = Option.empty[ItemMessage]
  private val itemBuffer = ListBuffer.empty[Message]
  logEvent = TraceEvent.VIEWPORT
  node.iterationPosition = 0L
  node.iterationSize = 0L

  override protected def input(port: String, message: Message): Unit = {
    if (port == "source") {
      message match {
        case item: ItemMessage =>
          sourceItem = Some(item)
          node.inputCardinalities.put(port, node.inputCardinalities.getOrElse(port, 0L) + 1)
        case _ =>
          throw JafplException.unexpectedMessage(message.toString, port, node.location)
      }
    } else {
      if (openOutputs.contains(port)) {
        itemBuffer += message
      } else {
        super.input(port, message)
      }
    }
  }

  override protected def reset(): Unit = {
    itemQueue.clear()
    index = 0
    sourceItem = None
    node.iterationPosition = 0L
    node.iterationSize = 0L
    super.reset()
  }

  override protected def run(): Unit = {
    if (node.iterationPosition == 0) {
      if (sourceItem.isDefined) {
        node.composer.runtimeBindings(bindings.toMap)
        for (item <- node.composer.decompose(sourceItem.get)) {
          itemQueue += item
        }
      }
      index = 0
      node.iterationSize = itemQueue.size
    }

    if (itemQueue.nonEmpty) {
      node.iterationPosition += 1
      stateChange(node, NodeState.RUNNING)
      val item = itemQueue(index)
      sendMessage("current", new PipelineMessage(item.getItem, item.getMetadata))
      sendClose("current")
      for (cnode <- node.children) {
        actors(cnode) ! NRunIfReady()
      }
    } else {
      closeOutputs()
      parent ! NFinished(node)
    }
  }

  override protected def finished(child: Node): Unit = {
    stateChange(child, NodeState.FINISHED)
    var finished = true
    for (cnode <- node.children) {
      finished = finished && cnode.state == NodeState.FINISHED
    }
    if (finished) {
      val item = itemQueue(index)
      val ibuffer = mutable.ListBuffer.empty[Any]
      for (item <- itemBuffer) {
        item match {
          case msg: ItemMessage =>
            ibuffer += msg.item
          case _: Message =>
            throw JafplException.internalError("Unexpected message $msg on returnItems in viewport", node.location)
            return
          case _ =>
            ibuffer += item
        }
      }

      item.putItems(ibuffer.toList)
      itemBuffer.clear()

      index += 1
      if (index >= itemQueue.size) {
        val recomposition = node.composer.recompose()
        sendMessage(node.outputPort, recomposition)
        closeOutputs()
        parent ! NFinished(node)
      } else {
        stateChange(node, NodeState.LOOPING)
        for (child <- node.children) {
          actors(child) ! NReset()
        }
      }
    } else {
      trace("UNFINISH", s"${nodeState(node)}", TraceEvent.STATECHANGE)
    }
  }
}
