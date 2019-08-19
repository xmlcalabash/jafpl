package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{ContainerStart, Node}
import com.jafpl.messages.{BindingMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GAbort, GClose, GException, GFinished, GOutput, GReset, GResetFinished, GStart, GStop, GStopped}
import com.jafpl.steps.Manifold

import scala.collection.mutable

private[runtime] class StartActor(private val monitor: ActorRef,
                                  override protected val runtime: GraphRuntime,
                                  override protected val node: ContainerStart) extends NodeActor(monitor, runtime, node)  {
  protected val bindings = mutable.HashMap.empty[String, Message]
  logEvent = TraceEvent.START
  
  protected def commonStart(): Unit = {
    started = childrenHaveReset
    for (inj <- node.stepInjectables) {
      inj.beforeRun()
    }
  }

  protected def commonFinished(): Unit = {
    for (inj <- node.stepInjectables) {
      inj.afterRun()
    }
  }

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", logEvent)

    if (port == "#bindings") {
      item match {
        case binding: BindingMessage =>
          for (inj <- node.stepInjectables) {
            inj.receiveBinding(binding)
          }
          bindings.put(binding.name, binding.message)
        case _ =>
          throw new RuntimeException("FIXME: bang")
      }
    }

    trace("FORWARD", s"$node.$port -> $item", logEvent)
    node.outputCardinalities.put(port, node.outputCardinalities.getOrElse(port, 0L) + 1)
    monitor ! GOutput(node, port, item)
  }

  protected def consume(port: String, item: Message): Unit = {
    throw new RuntimeException("Called consume on StartActor")
  }

  override protected def close(port: String): Unit = {
    if (openOutputs.contains(port)) {
      openOutputs -= port
      trace("CLOSEO", s"$node.$port: $childState : $openOutputs", logEvent)
      //monitor ! GClose(node, port)
      finishIfReady()
    } else {
      openInputs -= port
      trace("CLOSEI", s"$node / $port", logEvent)
      if (port == "#bindings") {
        if (bufferedInput.nonEmpty) {
          for (buf <- bufferedInput) {
            input(buf.from, buf.fromPort, buf.port, buf.item)
          }
          bufferedInput.clear()
        }
      }
      monitor ! GClose(node, port)
      runIfReady()
    }
  }

  // override protected def start(): Unit = {

  protected[runtime] def finished(): Unit = {
    trace("FINISHED", s"$node / ${node.state}", logEvent)
    if (node.state != NodeState.ABORTED) {
      node.state = NodeState.FINISHED
    }
    checkCardinalities()
    for (output <- node.outputs) {
      monitor ! GClose(node, output)
    }
    monitor ! GFinished(node)
    for (inj <- node.stepInjectables) {
      inj.afterRun()
    }
  }

  protected[runtime] def childFinished(child: Node): Unit = {
    childState(child) = child.state
    trace("CFINISH", s"$node: $childState : $openOutputs", logEvent)
    try {
      finishIfReady()
    } catch {
      case jex: JafplException =>
        monitor ! GException(Some(node), jex)
      case ex: Exception =>
        throw ex
    }
  }

  protected def finishIfReady(): Unit = {
    trace("FINRDY", s"$node children: $childrenHaveFinished  outputs: $openOutputs", logEvent)
    if (childrenHaveFinished && openOutputs.isEmpty) {
      finished()
    }
  }

  override protected def abort(): Unit = {
    trace("ABORT", s"$node", logEvent)
    aborted = true
    openOutputs.clear()
    for (child <- node.children) {
      monitor ! GAbort(child)
    }
  }

  override protected def stop(): Unit = {
    trace("STOP", s"$node", logEvent)
    //monitor ! GStop(node.containerEnd)
    for (child <- node.children) {
      monitor ! GStop(child)
    }
    monitor ! GStopped(node)
  }

  override protected def reset(): Unit = {
    bindings.clear()
    childState.clear()
    for (child <- node.children) {
      childState(child) = NodeState.RESETTING
      child.state = NodeState.RESETTING
      monitor ! GReset(child)
    }
    super.reset()
  }

  override protected def resetIfReady(): Unit = {
    var fchild = Option.empty[String]
    var count = 0
    for ((child,state) <- childState) {
      if (state == NodeState.RESETTING) {
        count += 1
        if (fchild.isEmpty) {
          fchild = Some(child.toString)
        }
      }
    }

    if (count > 0) {
      trace("RESET?", s"$node $count / ${fchild.get}", logEvent)
    } else {
      trace("RESET", s"$node", logEvent)
    }

    if (childrenHaveReset) {
      monitor ! GResetFinished(node)
    }
  }

  override protected def resetFinished(child: Node): Unit = {
    if (child == node.containerEnd || childState.contains(child)) {
      childState(child) = NodeState.RESET
      trace("CXSTATE", s"$node / $child : $childState", logEvent)
      resetIfReady()
    } else {
      trace("CXSTATE!", s"$node / $child : $childState", logEvent)
      throw new RuntimeException(s"Illegal state: $child is not a child of $node")
    }
  }

  override protected def run(): Unit = {
    trace("RUN", s"$node", logEvent)

    for (child <- node.children) {
      childState(child) = NodeState.STARTED
    }
    trace("CSTATE", s"$node / $childState", logEvent)

    for (child <- node.children) {
      monitor ! GStart(child)
    }

    node.state = NodeState.STARTED
  }

  protected[runtime] def checkCardinalities(): Unit = {
    checkCardinalities(List())
  }

  protected[runtime] def checkCardinalities(exclude: String): Unit = {
    checkCardinalities(List(exclude))
  }

  protected[runtime] def checkCardinalities(exclude: List[String]): Unit = {
    for (output <- node.outputs.filter(!_.startsWith("#"))) {
      if (!exclude.contains(output)) {
        val count = node.outputCardinalities.getOrElse(output, 0L)
        val ospec = node.manifold.getOrElse(Manifold.ALLOW_ANY)
        // If the port isn't explicitly defined, ignore it. (Sometimes extra ports exist because
        // on compound steps, you need inputs to be outputs and vice-versa in order to read
        // from them or write to them.)
        if (ospec.outputSpec.cardinality(output).isDefined) {
          ospec.outputSpec.checkOutputCardinality(output, count)
        }
      }
    }
    node.inputCardinalities.clear()
    node.outputCardinalities.clear()
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [StartActor]"
  }
}
