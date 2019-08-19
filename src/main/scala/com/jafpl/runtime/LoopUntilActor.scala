package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{LoopUntilStart, Node}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput, GRestartLoop}

import scala.collection.mutable

private[runtime] class LoopUntilActor(private val monitor: ActorRef,
                                      override protected val runtime: GraphRuntime,
                                      override protected val node: LoopUntilStart)
  extends LoopActor(monitor, runtime, node) {

  var currentItem = Option.empty[ItemMessage]
  var nextItem = Option.empty[ItemMessage]
  var looped = false
  logEvent = TraceEvent.LOOPUNTIL
  node.iterationPosition = 0L
  node.iterationSize = 0L

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", logEvent)
    if (port == "test") {
      nextItem = Some(item.asInstanceOf[ItemMessage])
      looped = true
    } else {
      if (node.returnAll || !openOutputs.contains(port)) {
        super.input(from, fromPort, port, item)
      }
    }
  }

  override protected def reset(): Unit = {
    node.iterationPosition = 0L
    node.iterationSize = 0L
    super.reset()
  }

  override protected[runtime] def finished(): Unit = {
    val finished =  try {
      node.comparator.areTheSame(currentItem.get.item, nextItem.get.item)
    } catch {
      case ex: Exception =>
        trace("FINISHED", s"$node ${currentItem.head} threw $ex", logEvent)
        monitor ! GException(Some(node), ex)
        return
    }

    if (finished) {
      trace("LOOPFIN", s"$node", logEvent)

      if (!node.returnAll) {
        monitor ! GOutput(node, "result", currentItem.get)
      }

      checkCardinalities("current")
      // now close the outputs
      for (output <- node.outputs) {
        if (!node.inputs.contains(output)) {
          // Don't close 'current'; it must have been closed to get here and re-closing
          // it propagates the close event to the steps and they shouldn't see any more
          // events!
          if (output != "current" && output != "test") {
            monitor ! GClose(node, output)
          }
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
      trace("ITERFIN", s"$node children:$childrenHaveFinished", logEvent)
      currentItem = nextItem
      nextItem = None
      monitor ! GRestartLoop(node)
    }
  }

  override protected def run(): Unit = {
    running = true
    node.state = NodeState.STARTED

    if (currentItem.isDefined) {
      node.iterationPosition += 1
      monitor ! GOutput(node, "current", currentItem.get)
      monitor ! GClose(node, "current")
      super.run()
    } else {
      finishIfReady()
    }
  }

  override def consume(port: String, item: Message): Unit = {
    trace("CONSUME", s"$node $port", logEvent)
    if (port == "source") {
      item match {
        case message: ItemMessage =>
          if (currentItem.isDefined) {
            monitor ! GException(None,
              JafplException.unexpectedSequence(node.toString, port, node.location))
            return
          }
          currentItem = Some(message)
        case _ =>
          monitor ! GException(None,
            JafplException.unexpectedMessage(item.toString, port, node.location))
          return
      }
    } else if (port == "#bindings") {
      item match {
        case msg: BindingMessage =>
          bindings.put(msg.name, msg)
        case _ =>
          monitor ! GException(None,
            JafplException.unexpectedMessage(item.toString, port, node.location))
          return
      }
    }
    runIfReady()
  }

  protected[runtime] def loop(item: ItemMessage): Unit = {
    trace("LOOP", s"$node", logEvent)
    nextItem = Some(item)
    looped = true
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [LoopUntil]"
  }
}
