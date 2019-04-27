package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{Node, ViewportStart}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message, PipelineMessage}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput, GReset, GStart}
import com.jafpl.steps.{DataConsumer, ViewportItem}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class ViewportActor(private val monitor: ActorRef,
                                     override protected val runtime: GraphRuntime,
                                     override protected val node: ViewportStart)
  extends StartActor(monitor, runtime, node) with DataConsumer {

  private val itemQueue = ListBuffer.empty[ViewportItem]
  private var running = false
  private var sourceClosed = false
  private var received = false
  private var index = 0

  override protected def start(): Unit = {
    trace("START", s"$node", TraceEvent.METHODS)
    commonStart()
    runIfReady()
  }

  override protected def reset(): Unit = {
    trace("RESET", s"$node", TraceEvent.METHODS)
    super.reset()
    running = false
    readyToRun = true
    runIfReady()
  }

  override protected def input(from: Node, fromPort: String, port: String, msg: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", TraceEvent.METHODS)
    receive(port, msg)
  }

  override def receive(port: String, msg: Message): Unit = {
    trace("RECEIVE", s"$node $port", TraceEvent.METHODS)

    msg match {
      case binding: BindingMessage => Unit
      case item: ItemMessage =>
        if (port == "source") {
          if (received) {
            monitor ! GException(None,
              JafplException.unexpectedSequence(node.toString, port, node.location))
            return
          }
          received = true
          for (item <- node.composer.decompose(item)) {
            itemQueue += item
          }
        }
      case _ =>
        monitor ! GException(None,
          JafplException.unexpectedMessage(msg.toString, port, node.location))
        return
    }

    runIfReady()
  }

  override protected def close(port: String): Unit = {
    trace("CLOSE", s"$node", TraceEvent.METHODS)
    sourceClosed = true
    runIfReady()
  }

  private def runIfReady(): Unit = {
    trace("RUNIFREADY", s"$node (running:$running ready:$readyToRun closed:$sourceClosed", TraceEvent.METHODS)

    if (!running && readyToRun && sourceClosed) {
      running = true
      if (itemQueue.nonEmpty) {
        val item = itemQueue(index)
        val edge = node.outputEdge("current")
        monitor ! GOutput(node, edge.fromPort, new PipelineMessage(item.getItem, item.getMetadata))
        monitor ! GClose(node, edge.fromPort)
        for (child <- node.children) {
          monitor ! GStart(child)
        }
      } else {
        finished()
      }
    }
  }

  protected[runtime] def returnItems(buffer: List[Any]): Unit = {
    val item = itemQueue(index)
    val ibuffer = mutable.ListBuffer.empty[Any]
    for (item <- buffer) {
      item match {
        case msg: ItemMessage =>
          ibuffer += msg.item
        case msg: Message =>
          monitor ! GException(None,
            JafplException.internalError("Unexpected message $msg on returnItems in viewport", node.location))
          return
        case _ =>
          ibuffer += item
      }
    }

    item.putItems(ibuffer.toList)
    index += 1
  }

  override protected[runtime] def finished(): Unit = {
    trace("FINISHED", s"$node source:$sourceClosed empty:${itemQueue.isEmpty}", TraceEvent.METHODS)
    if (sourceClosed) {
      if (itemQueue.isEmpty) {
        monitor ! GClose(node, node.outputPort)
        monitor ! GFinished(node)
        commonFinished()
      } else {
        if (index >= itemQueue.size) {
          trace("VFINISHED", s"$node source:$sourceClosed, index:$index size:${itemQueue.size}", TraceEvent.METHODS)
          // send the transformed result and close the output
          val recomposition = node.composer.recompose()
          trace("VRECOMP", s"$node $recomposition", TraceEvent.METHODS)
          monitor ! GOutput(node, node.outputPort, recomposition)
          monitor ! GClose(node, node.outputPort)
          monitor ! GFinished(node)
        } else {
          monitor ! GReset(node)
        }
      }
    } else {
      // how does finished get called before run, exactly?
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Viewport]"
  }
}
