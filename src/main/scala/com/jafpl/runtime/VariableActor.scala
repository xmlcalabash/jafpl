package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{Binding, Node, OptionBinding}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message, Metadata}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput}
import com.jafpl.steps.DataConsumer

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class VariableActor(private val monitor: ActorRef,
                                     override protected val runtime: GraphRuntime,
                                     private val binding: Binding)
  extends NodeActor(monitor, runtime, binding) with DataConsumer {
  private var exprContext = ListBuffer.empty[Message]
  private val bindings = mutable.HashMap.empty[String, Message]

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", TraceEvent.METHODS)
    receive(port, item)
  }

  override def receive(port: String, item: Message): Unit = {
    trace("RECEIVE", s"$port", TraceEvent.METHODS)

    item match {
      case item: ItemMessage =>
        assert(port == "source")
        trace("RECVBCTX", "", TraceEvent.BINDINGS)
        exprContext += item
      case binding: BindingMessage =>
        assert(port == "#bindings")
        trace("RECVBIND", s"${binding.name}=${binding.message}", TraceEvent.BINDINGS)
        bindings.put(binding.name, binding.message)
      case _ =>
        monitor ! GException(None,
          JafplException.unexpectedMessage(item.toString, port, binding.location))
    }
  }

  override protected def run(): Unit = {
    // Pass any statics in as normal bindings
    for ((binding,message) <- node.staticBindings) {
      if (!bindings.contains(binding.name)) {
        bindings.put(binding.name, message)
      }
    }

    trace("RUN", s"$node", TraceEvent.METHODS)
    if (runtime.traceEventManager.traceEnabled("Bindings")) {
      var sbindings = ""
      for (name <- bindings.keySet) {
        if (sbindings != "") {
          sbindings += ", "
        }
        sbindings += s"$name=${bindings(name)}"
      }
      if (sbindings != "") {
        sbindings = " (with " + sbindings + ")"
      }
      trace("COMPUTE", s"${binding.name}=${binding.expression}$sbindings", TraceEvent.BINDINGS)
    }

    binding match {
      case opt: OptionBinding =>
        if (opt.value.isDefined) {
          computedValue(opt.value.get)
        } else {
          computeValue()
        }
      case _ =>
        computeValue()
    }
  }

  private def computeValue(): Unit = {
    try {
      val expreval = runtime.runtime.expressionEvaluator.newInstance()
      val answer = if (binding.static) {
        runtime.getStatic(binding)
      } else {
        expreval.value(binding.expression, exprContext.toList, bindings.toMap, binding.options)
      }

      val msg = new BindingMessage(binding.name, answer)
      monitor ! GOutput(binding, "result", msg)
      monitor ! GClose(binding, "result")
      monitor ! GFinished(binding)
    } catch {
      case t: Throwable =>
        monitor ! GException(Some(binding), t)
    }
  }

  private def computedValue(value: Any): Unit = {
    // If a precomputed value is provided, it wins. Note that the caller is responsible
    // for dealing with static vs. dynamic values in this case.
    try {
      val expreval = runtime.runtime.expressionEvaluator.newInstance()
      val answer = expreval.precomputedValue(binding.expression, value, exprContext.toList, bindings.toMap, binding.options)

      val msg = new BindingMessage(binding.name, answer)
      monitor ! GOutput(binding, "result", msg)
      monitor ! GClose(binding, "result")
      monitor ! GFinished(binding)
    } catch {
      case t: Throwable =>
        monitor ! GException(Some(binding), t)
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Variable]"
  }
}
