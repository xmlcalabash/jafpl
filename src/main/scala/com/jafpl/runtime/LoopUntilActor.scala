package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{LoopUntilStart, Node}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput, GReset, GStart}
import com.jafpl.steps.DataConsumer

import scala.collection.mutable

private[runtime] class LoopUntilActor(private val monitor: ActorRef,
                                      private val runtime: GraphRuntime,
                                      private val node: LoopUntilStart)
  extends StartActor(monitor, runtime, node) with DataConsumer {

  var currentItem = Option.empty[ItemMessage]
  var nextItem = Option.empty[ItemMessage]
  var running = false
  var looped = false
  val bindings = mutable.HashMap.empty[String, Message]

  override protected def start(): Unit = {
    commonStart()
    runIfReady()
  }

  override protected def reset(): Unit = {
    super.reset()
    running = false
    readyToRun = true
    looped = false
    runIfReady()
  }

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    receive(port, item)
  }

  override def id: String = node.id
  override def receive(port: String, item: Message): Unit = {
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
    nextItem = Some(item)
    looped = true
  }

  override protected def close(port: String): Unit = {
    runIfReady()
  }

  private def runIfReady(): Unit = {
    trace(s"RUNIFR UntilFinished: $running $readyToRun ${currentItem.isDefined}", "UntilFinished")
    if (!running && readyToRun && currentItem.isDefined) {
      running = true

      val edge = node.outputEdge("current")
      monitor ! GOutput(node, edge.fromPort, currentItem.get)
      monitor ! GClose(node, edge.fromPort)

      trace(s"START UntilFinished: $node", "UntilFinished")

      for (child <- node.children) {
        trace(s"START ...$child (for $node)", "UntilFinished")
        monitor ! GStart(child)
      }
    }
  }

  override protected[runtime] def finished(): Unit = {
    val finished = node.comparator.areTheSame(currentItem.get.item, nextItem.get.item)

    trace(s"TESTE UntilFinished: " + currentItem.get + ": " + nextItem.get + ": " + finished, "UntilFinished")

    if (finished) {
      trace(s"FINISH UntilFinished", "UntilFinished")
      monitor ! GOutput(node, "result", nextItem.get)
      // now close the outputs
      for (output <- node.outputs) {
        if (!node.inputs.contains(output)) {
          monitor ! GClose(node, output)
        }
      }
      monitor ! GFinished(node)
      commonFinished()
    } else {
      trace(s"LOOPR UntilFinished", "UntilFinished")
      trace(s"RESET UntilFinished: $node", "UntilFinished")
      currentItem = nextItem
      nextItem = None
      monitor ! GReset(node)
    }
  }
}
