package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.{GraphException, PipelineException}
import com.jafpl.graph.{UntilFinishedStart, WhileStart}
import com.jafpl.messages.BindingMessage
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput, GReset, GStart}

import scala.collection.mutable

private[runtime] class UntilFinishedActor(private val monitor: ActorRef,
                                          private val runtime: GraphRuntime,
                                          private val node: UntilFinishedStart)
  extends StartActor(monitor, runtime, node)  {

  var currentItem = Option.empty[Any]
  var nextItem = Option.empty[Any]
  var running = false
  var looped = false
  val bindings = mutable.HashMap.empty[String, Any]

  override protected def start(): Unit = {
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

  override protected def input(port: String, item: Any): Unit = {
    if (port == "source") {
      if (currentItem.isDefined) {
        throw new PipelineException("seqinput", "UntilFinished received a sequence.")
      }
      currentItem = Some(item)
    } else if (port == "#bindings") {
      item match {
        case msg: BindingMessage =>
          bindings.put(msg.name, msg.item)
        case _ => throw new GraphException(s"Unexpected message on $port", node.location)
      }
    }
    runIfReady()
  }

  override def loop(item: Any): Unit = {
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

      val edge = node.outputEdge("source")
      monitor ! GOutput(node, edge.toPort, currentItem.get)
      monitor ! GClose(node, edge.toPort)

      trace(s"START UntilFinished: $node", "UntilFinished")

      for (child <- node.children) {
        trace(s"START ...$child (for $node)", "UntilFinished")
        monitor ! GStart(child)
      }
    }
  }

  override protected[runtime] def finished(): Unit = {
    val finished = node.comparator.areTheSame(currentItem.get, nextItem.get)

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
    } else {
      trace(s"LOOPR UntilFinished", "UntilFinished")
      trace(s"RESET UntilFinished: $node", "UntilFinished")
      currentItem = nextItem
      nextItem = None
      monitor ! GReset(node)
    }
  }
}
