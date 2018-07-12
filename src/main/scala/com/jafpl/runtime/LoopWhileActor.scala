package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{LoopWhileStart, Node}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput, GReset, GStart}
import com.jafpl.steps.DataConsumer

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class LoopWhileActor(private val monitor: ActorRef,
                                      private val runtime: GraphRuntime,
                                      private val node: LoopWhileStart)
  extends StartActor(monitor, runtime, node) with DataConsumer {

  private val currentItem = ListBuffer.empty[ItemMessage]
  var running = false
  var looped = false
  val bindings = mutable.HashMap.empty[String, Message]
  var initiallyTrue = true

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

  override protected def input(from: Node, fromPort: String, port: String, msg: Message): Unit = {
    receive(port, msg)
  }

  override def id: String = node.id
  override def receive(port: String, msg: Message): Unit = {
    msg match {
      case item: ItemMessage =>
        if (currentItem.nonEmpty) {
          monitor ! GException(None,
            JafplException.unexpectedSequence(node.toString, port, node.location))
          return
        }
        currentItem += item
        val testItem = ListBuffer.empty[Message]
        testItem += currentItem.head
        initiallyTrue = node.tester.test(testItem.toList, bindings.toMap)
        trace(s"INTRU While: $initiallyTrue", "While")
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
    currentItem.clear()
    currentItem += item
    looped = true
  }

  override protected def close(port: String): Unit = {
    runIfReady()
  }

  private def runIfReady(): Unit = {
    trace(s"RUNIFRDY $node (running:$running ready:$readyToRun current:${currentItem.nonEmpty}", "While")
    if (!running && readyToRun && currentItem.nonEmpty) {
      running = true

      if (initiallyTrue) {
        val edge = node.outputEdge("current")
        monitor ! GOutput(node, edge.fromPort, currentItem.head)
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
    val testItem = ListBuffer.empty[Message]
    testItem += currentItem.head
    val pass = node.tester.test(testItem.toList, bindings.toMap)

    trace(s"CHKWHILE condition: $pass: ${currentItem.head}", "While")

    if (pass) {
      monitor ! GReset(node)
    } else {
      monitor ! GOutput(node, "result", currentItem.head)
      // now close the outputs
      for (output <- node.outputs) {
        if (!node.inputs.contains(output)) {
          monitor ! GClose(node, output)
        }
      }
      monitor ! GFinished(node)
      commonFinished()
    }
  }
}
