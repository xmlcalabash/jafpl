package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.{Binding, Node, OptionBinding}
import com.jafpl.messages.{BindingMessage, ItemMessage, Message, Metadata}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput}
import com.jafpl.steps.DataConsumer

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class VariableActor(private val monitor: ActorRef,
                                     private val runtime: GraphRuntime,
                                     private val binding: Binding)
  extends NodeActor(monitor, runtime, binding) with DataConsumer {
  private var exprContext = ListBuffer.empty[Message]
  private val bindings = mutable.HashMap.empty[String, Message]

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    receive(port, item)
  }

  override def id: String = binding.id
  override def receive(port: String, item: Message): Unit = {
    item match {
      case item: ItemMessage =>
        assert(port == "source")
        trace(s"RECEIVED binding context", "Bindings")
        exprContext += item
      case binding: BindingMessage =>
        assert(port == "#bindings")
        trace(s"RECVBIND ${binding.name}=${binding.message}", "Bindings")
        bindings.put(binding.name, binding.message)
      case _ =>
        monitor ! GException(None,
          new PipelineException("badmessage", s"Unexpected message on $item on $port", binding.location))
        return
    }
  }

  override protected def run(): Unit = {
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
      trace(s"COMPUTE= ${binding.name}=${binding.expression}$sbindings", "Bindings")
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
      val answer = expreval.value(binding.expression, exprContext.toList, bindings.toMap, binding.options)

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
}
