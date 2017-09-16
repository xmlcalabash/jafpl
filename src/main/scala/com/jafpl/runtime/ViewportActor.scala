package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.{Node, ViewportStart}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message, PipelineMessage}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput, GReset, GStart}
import com.jafpl.steps.{DataConsumer, ViewportItem}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class ViewportActor(private val monitor: ActorRef,
                                     private val runtime: GraphRuntime,
                                     private val node: ViewportStart)
  extends StartActor(monitor, runtime, node) with DataConsumer {

  private val itemQueue = ListBuffer.empty[ViewportItem]
  private var running = false
  private var sourceClosed = false
  private var received = false
  private var index = 0

  override protected def start(): Unit = {
    readyToRun = true
    runIfReady()
  }

  override protected def reset(): Unit = {
    super.reset()
    running = false
    readyToRun = true
    runIfReady()
  }

  override protected def input(from: Node, fromPort: String, port: String, msg: Message): Unit = {
    runtime.runtime.deliver(from.id, fromPort, msg, this, port)
  }

  override def id: String = node.id
  override def receive(port: String, msg: Message): Unit = {
    msg match {
      case binding: BindingMessage => Unit
      case item: ItemMessage =>
        if (port == "source") {
          if (received) {
            monitor ! GException(None,
              new PipelineException("noseq", "Sequence not allowed on viewport", node.location))
            return
          }
          received = true
          for (item <- node.composer.decompose(item)) {
            itemQueue += item
          }
        }
      case _ =>
        monitor ! GException(None,
          new PipelineException("badmessage", s"Unexpected message on $msg on $port", node.location))
        return
    }

    runIfReady()
  }

  override protected def close(port: String): Unit = {
    sourceClosed = true
    runIfReady()
  }

  private def runIfReady(): Unit = {
    trace(s"RUNIFRDY $node (running:$running ready:$readyToRun closed:$sourceClosed", "Viewport")

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
            new PipelineException("badmessage", s"Unexpected message on returnItems", node.location))
          return
        case _ =>
          ibuffer += item
      }
    }

    item.putItems(ibuffer.toList)
    index += 1
  }

  override protected[runtime] def finished(): Unit = {
    if (sourceClosed) {
      if (itemQueue.isEmpty) {
        monitor ! GClose(node, node.outputPort)
        monitor ! GFinished(node)
      } else {
        if (index >= itemQueue.size) {
          trace(s"FINISHED $node source:$sourceClosed, queue:${itemQueue.isEmpty}", "Viewport")
          // send the transformed result and close the output
          val recomposition = node.composer.recompose()
          trace(s"RECOMP: $recomposition", "X")
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
}
