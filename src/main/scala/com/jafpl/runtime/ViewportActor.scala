package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.ViewportStart
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput, GReset, GStart}
import com.jafpl.steps.ViewportItem

import scala.collection.mutable.ListBuffer

private[runtime] class ViewportActor(private val monitor: ActorRef,
                                     private val runtime: GraphRuntime,
                                     private val node: ViewportStart) extends StartActor(monitor, runtime, node)  {
  val queue = ListBuffer.empty[ViewportItem]
  var running = false
  var sourceClosed = false
  var received = false
  var index = 0

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

  override protected def input(port: String, item: Any): Unit = {
    if (port == "source") {
      if (received) {
        throw new PipelineException("NoSeq", "Sequence not allowed on viewport")
      }
      received = true
      for (item <- node.composer.decompose(item)) {
        queue += item
      }
    }

    runIfReady()
  }

  override protected def close(port: String): Unit = {
    sourceClosed = true
    runIfReady()
  }

  private def runIfReady(): Unit = {
    if (!running && readyToRun && queue.nonEmpty) {
      running = true

      // FIXME: What if the queue is empty

      val item = queue(index)
      val edge = node.outputEdge("source")
      monitor ! GOutput(node, edge.toPort, item.getItem)
      monitor ! GClose(node, edge.toPort)

      trace(s"START Viewport: $node", "Viewport")

      for (child <- node.children) {
        trace(s"START ...$child (for $node)", "Viewport")
        monitor ! GStart(child)
      }
    }
  }

  protected[runtime] def returnItems(buffer: List[Any]): Unit = {
    val item = queue(index)
    item.putItems(buffer)
    index += 1
  }

  override protected[runtime] def finished(): Unit = {
    if (sourceClosed && (index >= queue.size)) {
      trace(s"FINISH Viewport: $node $sourceClosed, ${queue.isEmpty}", "Viewport")
      // send the transformed result and close the output
      monitor ! GOutput(node, "result", node.composer.recompose())
      monitor ! GClose(node, "result")
      monitor ! GFinished(node)
    } else {
      trace(s"RESET Viewport: $node $sourceClosed, ${queue.isEmpty}", "Viewport")
      monitor ! GReset(node)
    }
  }
}
