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
  extends LoopActor(monitor, runtime, node) with DataConsumer {

  var currentItem = Option.empty[ItemMessage]
  var looped = false
  var loopThisTime = true
  logEvent = TraceEvent.LOOPWHILE
  node.iterationPosition = 0L
  node.iterationSize = 0L

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", logEvent)
    if (port == "test") {
      currentItem = Some(item.asInstanceOf[ItemMessage])
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
    val pass = try {
      node.tester.test(List(currentItem.get), bindings.toMap)
    } catch {
      case ex: Exception =>
        trace("FINISHED", s"$node ${currentItem.head} threw $ex", logEvent)
        monitor ! GException(Some(node), ex)
        return
    }

    if (pass) {
      trace("ITERFIN", s"$node children:$childrenHaveFinished", logEvent)
      monitor ! GRestartLoop(node)
    } else {
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
    }
  }

  override protected def run(): Unit = {
    running = true
    node.state = NodeState.STARTED

    trace("WHILERUN", s"$node: $childState : $openOutputs $loopThisTime", logEvent)

    if (loopThisTime) {
      node.iterationPosition += 1
      monitor ! GOutput(node, "current", currentItem.get)
      monitor ! GClose(node, "current")
      super.run()
    } else {
      finishIfReady()
    }
  }

  override def consume(port: String, msg: Message): Unit = {
    trace("CONSUME", s"$node $port", logEvent)
    msg match {
      case item: ItemMessage =>
        if (currentItem.nonEmpty) {
          monitor ! GException(None,
            JafplException.unexpectedSequence(node.toString, port, node.location))
          return
        }
        currentItem = Some(item)
        val testItem = List(item)
        loopThisTime = node.tester.test(testItem, bindings.toMap)
        trace("RECVTRUE", s"$node while: $loopThisTime", logEvent)
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

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [LoopWhile]"
  }
}
