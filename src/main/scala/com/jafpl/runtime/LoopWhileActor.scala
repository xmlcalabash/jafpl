package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{LoopWhileStart, Node}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput, GReset, GRestartLoop, GStart}
import com.jafpl.steps.DataConsumer

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class LoopWhileActor(private val monitor: ActorRef,
                                      override protected val runtime: GraphRuntime,
                                      override protected val node: LoopWhileStart)
  extends StartActor(monitor, runtime, node) with DataConsumer {

  var currentItem = Option.empty[ItemMessage]
  var running = false
  var looped = false
  val bindings = mutable.HashMap.empty[String, Message]
  var initiallyTrue = true
  logEvent = TraceEvent.LOOPWHILE
  node.iterationPosition = 0L
  node.iterationSize = 0L

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
    looped = false
    node.iterationPosition = 0L
    node.iterationSize = 0L
    runIfReady()
  }

  protected[runtime] def restartLoop(): Unit = {
    trace("RSTRTLOOP", s"$node", logEvent)
    super.reset() // yes, reset
    running = false
    readyToRun = true
    looped = false
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
        if (currentItem.nonEmpty) {
          monitor ! GException(None,
            JafplException.unexpectedSequence(node.toString, port, node.location))
          return
        }
        currentItem = Some(item)
        val testItem = List(item)
        initiallyTrue = node.tester.test(testItem, bindings.toMap)
        trace("RECVTRUE", s"$node while: $initiallyTrue", logEvent)
      case item: BindingMessage =>
        bindings.put(item.name, item)
      case _ =>
        monitor ! GException(None,
          JafplException.unexpectedMessage(msg.toString, port, node.location))
        return
    }
    runIfReady()
  }

  protected[runtime] def loop(item: ItemMessage): Unit = {
    trace("LOOP", s"$node", logEvent)
    currentItem = Some(item)
    looped = true
  }

  override protected def close(port: String): Unit = {
    trace("CLOSE", s"$node", logEvent)
    runIfReady()
  }

  private def runIfReady(): Unit = {
    trace("RUNIFREADY", s"$node ready:$readyToRun running:$running current:${currentItem.nonEmpty}", logEvent)
    if (!running && readyToRun && currentItem.nonEmpty) {
      running = true

      node.iterationPosition += 1
      node.iterationSize += 1

      if (initiallyTrue) {
        val edge = node.outputEdge("current")
        monitor ! GOutput(node, edge.fromPort, currentItem.get)
        monitor ! GClose(node, edge.fromPort)
        for (child <- node.children) {
          monitor ! GStart(child)
        }
      } else {
        finished()
      }
    }
  }

  override protected[runtime] def finished(): Unit = {
    val pass = try {
      node.tester.test(List(currentItem.get), bindings.toMap)
    } catch {
      case ex: Exception =>
        trace("FINISHED", s"$node ${currentItem.head} threw $ex", logEvent)
        monitor ! GException(Some(node), ex)
        return
    }

    trace("FINISHED", s"$node $pass ${currentItem.head}", logEvent)

    if (pass) {
      monitor ! GRestartLoop(node)
    } else {
      checkCardinalities("current")

      for (port <- node.buffer.keySet) {
        for (item <- node.buffer(port)) {
          node.outputCardinalities.put(port, node.outputCardinalities.getOrElse(port, 0L) + 1)
          monitor ! GOutput(node, port, item)
        }
      }
      node.buffer.clear()

      // now close the outputs
      for (output <- node.outputs) {
        if (!node.inputs.contains(output)) {
          // Don't close 'current'; it must have been closed to get here and re-closing
          // it propagates the close event to the steps and they shouldn't see any more
          // events!
          if (output != "current") {
            monitor ! GClose(node, output)
          }
        }
      }
      monitor ! GFinished(node)
      commonFinished()
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [LoopWhile]"
  }
}
