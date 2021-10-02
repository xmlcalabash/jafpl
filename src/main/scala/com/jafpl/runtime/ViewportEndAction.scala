package com.jafpl.runtime

import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{ContainerEnd, ViewportStart}
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.NodeState.NodeState
import com.jafpl.util.TraceEventManager

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class ViewportEndAction(override val node: ContainerEnd) extends EndAction(node) {
  private var startAction: ViewportAction = _
  private val viewportStart: ViewportStart = node.start.get.asInstanceOf[ViewportStart]
  private val itemBuffer = ListBuffer.empty[Message]
  private var outputPort = "result"

  def loopStartAction: ViewportAction = startAction
  def loopStartAction_=(start: ViewportAction): Unit = {
    startAction = start
    outputPort = start.node.outputPort
  }

  override def receive(port: String, message: Message): Unit = {
    outputPort = port
    itemBuffer += message
  }

  override def run(): Unit = {
    tracer.trace(s"RUN   $this ************************************************************", TraceEventManager.RUN)

    if (startAction.itemQueue.nonEmpty) {
      val item = startAction.itemQueue(startAction.index)
      val ibuffer = mutable.ListBuffer.empty[Any]
      for (item <- itemBuffer) {
        item match {
          case msg: ItemMessage =>
            ibuffer += msg.item
          case _: Message =>
            throw JafplException.internalError("Unexpected message $msg on returnItems in viewport", node.location)
          case _ =>
            ibuffer += item
        }
      }

      try {
        item.putItems(ibuffer.toList)
      } catch {
        case t: Throwable =>
          println(s"BANG! $t")
          throw t
      }
    }

    if (startAction.receivedInput && startAction.finished()) {
      val recomposition = viewportStart.composer.recompose()
      scheduler.receive(node, outputPort, recomposition)
    }

    scheduler.finish(node)
    itemBuffer.clear()
  }

  override def reset(state: NodeState): Unit = {
    super.reset(state)
    itemBuffer.clear()
  }
}
