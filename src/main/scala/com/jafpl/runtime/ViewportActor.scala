package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{Node, ViewportStart}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message, PipelineMessage}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput, GReset, GRestartLoop, GStart}
import com.jafpl.steps.{Manifold, ViewportItem}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class ViewportActor(private val monitor: ActorRef,
                                     override protected val runtime: GraphRuntime,
                                     override protected val node: ViewportStart)
  extends LoopActor(monitor, runtime, node) {

  private val itemQueue = ListBuffer.empty[ViewportItem]
  private var index = 0
  private var sourceItem = Option.empty[ItemMessage]
  private val buffer = ListBuffer.empty[Message]
  logEvent = TraceEvent.VIEWPORT

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", logEvent)
    if (port == "source") {
      consume(port, item)
    } else if (openOutputs.contains(port)) {
      buffer += item
    } else {
      super.input(from, fromPort, port, item)
    }
  }

  override protected[runtime] def finished(): Unit = {
    if (itemQueue.isEmpty) {
      trace("VIEWPFIN", s"$node", logEvent)
      checkCardinalities("current")

      // now close the outputs
      for (output <- node.outputs) {
        // Don't close 'current'; it must have been closed to get here and re-closing
        // it propagates the close event to the steps and they shouldn't see any more
        // events!
        if (output != "current") {
          trace("XYZ", s"$node 1 $output", logEvent)
          monitor ! GClose(node, output)
        }
      }

      if (childrenHaveFinished) {
        node.state = NodeState.FINISHED
        monitor ! GFinished(node)
        for (inj <- node.stepInjectables) {
          inj.afterRun()
        }
      }
    } else {
      if (index >= itemQueue.size) {
        trace("VFINISHED", s"$node source:${openInputs}, index:$index size:${itemQueue.size}", logEvent)
        // send the transformed result and close the output
        val recomposition = node.composer.recompose()
        trace("VRECOMP", s"$node $recomposition", logEvent)
        monitor ! GOutput(node, node.outputPort, recomposition)
        monitor ! GClose(node, node.outputPort)
        node.state = NodeState.FINISHED
        monitor ! GFinished(node)
        for (inj <- node.stepInjectables) {
          inj.afterRun()
        }
        index = 0
      } else {
        trace("VITER", s"$node children:$childrenHaveFinished", logEvent)
        if (childrenHaveFinished) {
          monitor ! GRestartLoop(node)
        }
      }
    }
  }

  override protected def reset(): Unit = {
    itemQueue.clear()
    super.reset()
  }

  override protected def run(): Unit = {
    trace("RUN", s"$node running:$running ready:$started", logEvent)

    running = true
    node.state = NodeState.STARTED

    if (node.iterationSize == 0) {
      if (sourceItem.isDefined) {
        node.composer.runtimeBindings(bindings.toMap)
        for (item <- node.composer.decompose(sourceItem.get)) {
          itemQueue += item
        }
      }
      index = 0
      node.iterationSize = itemQueue.size

      val port = "source"
      val count = node.inputCardinalities.getOrElse(port, 0L)
      val ispec = node.manifold.getOrElse(Manifold.ALLOW_ANY)
      trace("CARD", s"$node input $port: $count", TraceEvent.CARDINALITY)
      ispec.inputSpec.checkInputCardinality(port, count)

      if (itemQueue.nonEmpty) {
        for (inj <- node.stepInjectables) {
          inj.beforeRun()
        }
      }
    }

    if (index < itemQueue.size) {
      val item = itemQueue(index)
      node.iterationPosition += 1
      val edge = node.outputEdge("current")
      monitor ! GOutput(node, edge.fromPort, new PipelineMessage(item.getItem, item.getMetadata))
      monitor ! GClose(node, edge.fromPort)
      super.run()
    } else {
      for (port <- openOutputs) {
        monitor ! GClose(node, port)
      }
      openOutputs.clear()

      finishIfReady()
    }
  }

  override protected def close(port: String): Unit = {
    trace("CLOSE", s"$node", logEvent)
    if (port == "source" || port == "#bindings") {
      openInputs -= port
      runIfReady()
    } else if (openOutputs.contains(port)) {
      returnItems(buffer.toList)
      buffer.clear()
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

  override def consume(port: String, item: Message): Unit = {
    trace("RECEIVE", s"$node $port", logEvent)
    item match {
      case message: ItemMessage =>
        sourceItem = Some(message)
        node.inputCardinalities.put(port, node.inputCardinalities.getOrElse(port, 0L) + 1)
      case _ =>
        monitor ! GException(Some(node), JafplException.unexpectedMessage(item.toString, port, node.location))
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Viewport]"
  }
}
