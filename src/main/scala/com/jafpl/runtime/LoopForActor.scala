package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.{LoopForStart, Node}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message, Metadata}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput, GReset, GStart}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class LoopForActor(private val monitor: ActorRef,
                                    private val runtime: GraphRuntime,
                                    private val node: LoopForStart)
  extends StartActor(monitor, runtime, node)  {

  private var current = node.countFrom
  var running = false
  var looped = false
  val bindings = mutable.HashMap.empty[String, Any]

  override protected def start(): Unit = {
    for (child <- node.children) {
      trace(s"........ HASCHILD $child", "ForLoop")
    }

    readyToRun = true
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
    throw PipelineException.INTERNALERR(s"No input is expected on a for loop; input arrived on $port", node.location)
  }

  protected[runtime] def loop(item: ItemMessage): Unit = {
    looped = true
  }

  override protected def close(port: String): Unit = {
    throw new PipelineException("noclose", "No port closures are expected on a for-loop", node.location)
  }

  private def runIfReady(): Unit = {
    trace(s"RUNIFRDY $node (running:$running ready:$readyToRun)", "ForLoop")
    if (!running && readyToRun) {
      running = true

      val initiallyTrue = if (node.countBy > 0) {
        current <= node.countTo
      } else {
        current >= node.countTo
      }

      trace(s"INIFLOOP initially: $initiallyTrue: $current", "ForLoop")

      if (initiallyTrue) {
        for (port <- node.outputs) {
          if (port == "current") {
            monitor ! GOutput(node, port, new ItemMessage(current, Metadata.NUMBER))
            monitor ! GClose(node, port)
          }
        }
        for (child <- node.children) {
          trace(s"........ START $child", "ForLoop")
          monitor ! GStart(child)
        }
      } else {
        finished()
      }
    }
  }

  override protected[runtime] def finished(): Unit = {
    current += node.countBy

    val pass = if (node.countBy > 0) {
      current <= node.countTo
    } else {
      current >= node.countTo
    }

    trace(s"CHKFLOOP condition: $pass: $current", "ForLoop")

    if (pass) {
      //monitor ! GOutput(node, "test", new ItemMessage(current, Metadata.NUMBER))
      //monitor ! GClose(node, "test")
      monitor ! GReset(node)
    } else {
      // now close the outputs
      for (output <- node.outputs) {
        if (!node.inputs.contains(output)) {
          monitor ! GClose(node, output)
        }
      }
      monitor ! GFinished(node)
    }
  }
}
