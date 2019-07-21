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
  private var inputsClosed = false
  private var received = false
  private var index = 0
  private var hasBindings = node.inputs.contains("#bindings")
  private var bindingsClosed = !hasBindings
  private var sourceClosed = false
  private val bindings = mutable.HashMap.empty[String, Message]
  private var sourceItem = Option.empty[ItemMessage]
  logEvent = TraceEvent.VIEWPORT

  override protected def start(): Unit = {
    trace("START", s"$node", logEvent)
    commonStart()
    runIfReady()
  }

  override protected def reset(): Unit = {
    trace("RESET", s"$node", logEvent)
    super.reset()
    running = false
    readyToRun = true
    bindings.clear()
    sourceItem = None
    runIfReady()
  }

  override protected def input(from: Node, fromPort: String, port: String, msg: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", logEvent)
    receive(port, msg)
  }

  override def receive(port: String, msg: Message): Unit = {
    trace("RECEIVE", s"$node $port", logEvent)

    msg match {
      case item: ItemMessage =>
        if (port != "source") {
          monitor ! GException(None,
            JafplException.unexpectedMessage(msg.toString, port, node.location))
          return
        }
        if (received) {
          monitor ! GException(None,
            JafplException.unexpectedSequence(node.toString, port, node.location))
          return
        }
        received = true
        sourceItem = Some(item)
      case binding: BindingMessage =>
        bindings.put(binding.name, binding.message)
      case _ =>
        return
    }

    runIfReady()
  }

  override protected def close(port: String): Unit = {
    trace("CLOSE", s"$node", logEvent)
    if (port == "source") {
      sourceClosed = true
    } else {
      bindingsClosed = true
      node.composer.runtimeBindings(bindings.toMap)
    }
    if (sourceClosed && bindingsClosed) {
      if (sourceItem.isDefined) {
        node.composer.runtimeBindings(bindings.toMap)
        for (item <- node.composer.decompose(sourceItem.get)) {
          itemQueue += item
        }
      }
      inputsClosed = true
    }
    runIfReady()
  }

  private def runIfReady(): Unit = {
    trace("RUNIFREADY", s"$node (running:$running ready:$readyToRun closed:$inputsClosed", logEvent)

    if (!running && readyToRun && inputsClosed) {
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
    trace("FINISHED", s"$node inputs:$inputsClosed empty:${itemQueue.isEmpty}", logEvent)
    if (inputsClosed) {
      if (itemQueue.isEmpty) {
        monitor ! GClose(node, node.outputPort)
        monitor ! GFinished(node)
        commonFinished()
      } else {
        if (index >= itemQueue.size) {
          trace("VFINISHED", s"$node source:$inputsClosed, index:$index size:${itemQueue.size}", logEvent)
          // send the transformed result and close the output
          val recomposition = node.composer.recompose()
          trace("VRECOMP", s"$node $recomposition", logEvent)
          monitor ! GOutput(node, node.outputPort, recomposition)
          monitor ! GClose(node, node.outputPort)
          monitor ! GFinished(node)
          index = 0
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
