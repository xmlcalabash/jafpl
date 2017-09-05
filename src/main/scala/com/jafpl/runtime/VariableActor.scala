package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.Binding
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class VariableActor(private val monitor: ActorRef,
                                     private val runtime: GraphRuntime,
                                     private val binding: Binding)
  extends NodeActor(monitor, runtime, binding)  {
  var exprContext = ListBuffer.empty[Any]
  val bindings = mutable.HashMap.empty[String, Any]

  override protected def input(port: String, msg: Message): Unit = {
    msg match {
      case item: ItemMessage =>
        assert(port == "source")
        trace(s"RECEIVED binding context", "Bindings")
        exprContext += item.item
      case binding: BindingMessage =>
        assert(port == "#bindings")
        trace(s"RECVBIND ${binding.name}=${binding.item}", "Bindings")
        bindings.put(binding.name, binding.item)
        openBindings -= binding.name
      case _ =>
        monitor ! GException(None,
          new PipelineException("badmessage", s"Unexpected message on $msg on $port", binding.location))
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

    val expreval = runtime.runtime.expressionEvaluator

    val answer = expreval.value(binding.expression.get, exprContext.toList, bindings.toMap)

    val msg = new BindingMessage(binding.name, answer)
    monitor ! GOutput(binding, "result", msg)
    monitor ! GClose(binding, "result")
    monitor ! GFinished(binding)
  }
}
