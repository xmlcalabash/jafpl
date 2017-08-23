package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.{GraphException, PipelineException}
import com.jafpl.graph.WhileStart
import com.jafpl.messages.BindingMessage
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput, GReset, GStart}

import scala.collection.mutable

private[runtime] class WhileActor(private val monitor: ActorRef,
                                  private val runtime: GraphRuntime,
                                  private val node: WhileStart) extends StartActor(monitor, runtime, node)  {
  var currentItem = Option.empty[Any]
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
        throw new PipelineException("seqinput", "While received a sequence.")
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
    currentItem = Some(item)
    looped = true
  }

  override protected def close(port: String): Unit = {
    runIfReady()
  }

  private def runIfReady(): Unit = {
    trace(s"RUNIFR While: $running $readyToRun ${currentItem.isDefined}", "While")
    if (!running && readyToRun && currentItem.isDefined) {
      running = true

      val edge = node.outputEdge("source")
      monitor ! GOutput(node, edge.toPort, currentItem.get)
      monitor ! GClose(node, edge.toPort)

      trace(s"START While: $node", "While")

      for (child <- node.children) {
        trace(s"START ...$child (for $node)", "While")
        monitor ! GStart(child)
      }
    }
  }

  override protected[runtime] def finished(): Unit = {
    val pass = runtime.dynamicContext.expressionEvaluator()
      .booleanValue(node.testexpr, currentItem, Some(bindings.toMap))

    trace(s"TESTE While: " + node.testexpr + ": " + currentItem.get + ": " + pass, "While")

    if (pass) {
      trace(s"LOOPR While", "While")
      trace(s"RESET While: $node", "While")
      monitor ! GReset(node)
    } else {
      trace(s"FINISH While", "While")
      monitor ! GOutput(node, "result", currentItem.get)
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
