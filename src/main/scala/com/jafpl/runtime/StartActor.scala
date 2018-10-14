package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{ContainerStart, Node}
import com.jafpl.messages.{BindingMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GAbort, GClose, GException, GFinished, GOutput, GReset, GStart, GStop, GStopped}
import com.jafpl.steps.Manifold

private[runtime] class StartActor(private val monitor: ActorRef,
                                  override protected val runtime: GraphRuntime,
                                  override protected val node: ContainerStart) extends NodeActor(monitor, runtime, node)  {
  protected def commonStart(): Unit = {
    readyToRun = true
    for (inj <- node.stepInjectables) {
      inj.beforeRun()
    }
  }

  protected def commonFinished(): Unit = {
    for (inj <- node.stepInjectables) {
      inj.afterRun()
    }
  }

  override protected def start(): Unit = {
    trace("START", s"$node", TraceEvent.METHODS)
    commonStart()
    for (child <- node.children) {
      monitor ! GStart(child)
    }
  }

  override protected def abort(): Unit = {
    trace("ABORT", s"$node", TraceEvent.METHODS)
    for (child <- node.children) {
      monitor ! GAbort(child)
    }
    monitor ! GFinished(node)
  }

  override protected def stop(): Unit = {
    trace("STOP", s"$node", TraceEvent.METHODS)
    for (child <- node.children) {
      monitor ! GStop(child)
    }
    monitor ! GStop(node.containerEnd)
    monitor ! GStopped(node)
  }

  override protected def reset(): Unit = {
    trace("RESET", s"$node", TraceEvent.METHODS)
    readyToRun = false

    monitor ! GReset(node.containerEnd)
    for (child <- node.children) {
      monitor ! GReset(child)
    }
  }

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", TraceEvent.METHODS)

    // Bindings on compound steps are only ever for injectables
    if (port == "#bindings") {
      for (inj <- node.stepInjectables) {
        item match {
          case binding: BindingMessage =>
            inj.receiveBinding(binding)
          case _ =>
            throw new RuntimeException("FIXME: bang")
        }
      }
    } else {
      val edge = node.outputEdge(port)
      trace("STRTSEND", s"Start actor $node sends to ${edge.toPort}: $item", TraceEvent.STEPIO)
      node.outputCardinalities.put(port, node.outputCardinalities.getOrElse(port, 0L) + 1)
      monitor ! GOutput(node, edge.fromPort, item)
    }
  }

  protected[runtime] def checkCardinalities(): Unit = {
    checkCardinalities(List.empty[String])
  }

  protected[runtime] def checkCardinalities(exclude: String): Unit = {
    checkCardinalities(List(exclude))
  }

  protected[runtime] def checkCardinalities(exclude: List[String]): Unit = {
    for (output <- node.outputs.filter(!_.startsWith("#"))) {
      if (!exclude.contains(output)) {
        val count = node.outputCardinalities.getOrElse(output, 0L)
        val ospec = node.manifold.getOrElse(Manifold.ALLOW_ANY)
        try {
          // If the port isn't explicitly defined, ignore it. (Sometimes extra ports exist because
          // on compound steps, you need inputs to be outputs and vice-versa in order to read
          // from them or write to them.)
          if (ospec.outputSpec.cardinality(output).isDefined) {
            ospec.outputSpec.checkOutputCardinality(output, count)
          }
        } catch {
          case jex: JafplException =>
            monitor ! GException(Some(node), jex)
        }
      }
    }
    node.inputCardinalities.clear()
    node.outputCardinalities.clear()
  }

  protected[runtime] def finished(): Unit = {
    trace("FINISHED", s"$node", TraceEvent.METHODS)
    checkCardinalities()
    for (output <- node.outputs) {
      monitor ! GClose(node, output)
    }
    monitor ! GFinished(node)
    commonFinished()
  }


  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [StartActor]"
  }
}
