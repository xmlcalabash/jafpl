package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.ViewportStart
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput, GReset, GStart}
import com.jafpl.steps.ViewportItem
import com.jafpl.util.PipelineMessage

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class ViewportActor(private val monitor: ActorRef,
                                     private val runtime: GraphRuntime,
                                     private val node: ViewportStart) extends StartActor(monitor, runtime, node)  {
  private val queue = ListBuffer.empty[ViewportItem]
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

  override protected def input(port: String, msg: Message): Unit = {
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
          for (item <- node.composer.decompose(item.item)) {
            queue += item
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
    if (!running && readyToRun && sourceClosed) {
      running = true

      if (queue.nonEmpty) {
        val item = queue(index)
        val edge = node.outputEdge("source")
        monitor ! GOutput(node, edge.toPort, new PipelineMessage(item.getItem))
        monitor ! GClose(node, edge.toPort)

        trace(s"START Viewport: $node", "Viewport")

        for (child <- node.children) {
          trace(s"START ...$child (for $node)", "Viewport")
          monitor ! GStart(child)
        }
      } else {
        finished()
      }
    }
  }

  protected[runtime] def returnItems(buffer: List[Any]): Unit = {
    val item = queue(index)
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
      if (queue.isEmpty) {
        monitor ! GClose(node, "result")
        monitor ! GFinished(node)
      } else {
        if (index >= queue.size) {
          trace(s"FINISH Viewport: $node $sourceClosed, ${queue.isEmpty}", "Viewport")
          // send the transformed result and close the output
          val recomposition = node.composer.recompose()
          recomposition match {
            case item: ItemMessage =>
              monitor ! GOutput(node, "result", item)
            case item: Message =>
              monitor ! GException(None,
                new PipelineException("badrecomp", "Invalid recomposition", node.location))
              return
            case _ =>
              monitor ! GOutput(node, "result", new PipelineMessage(recomposition))
          }
          monitor ! GClose(node, "result")
          monitor ! GFinished(node)
        } else {
          trace(s"RESET Viewport: $node $sourceClosed, ${queue.isEmpty}", "Viewport")
          monitor ! GReset(node)
        }
      }
    } else {
      // how does finished get called before run, exactly?
    }
  }
}