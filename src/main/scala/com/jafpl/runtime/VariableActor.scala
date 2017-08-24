package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.Binding
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput}

import scala.collection.mutable

private[runtime] class VariableActor(private val monitor: ActorRef,
                                     private val runtime: GraphRuntime,
                                     private val binding: Binding)
  extends NodeActor(monitor, runtime, binding)  {
  var exprContext = Option.empty[ItemMessage]
  val bindings = mutable.HashMap.empty[String, Any]

  override protected def input(port: String, msg: Message): Unit = {
    msg match {
      case item: ItemMessage =>
        assert(port == "source")
        trace(s"BCNXT $item", "Bindings")
        exprContext = Some(item)
      case binding: BindingMessage =>
        assert(port == "#bindings")
        trace(s"BOUND ${binding.name}=${binding.item}", "Bindings")
        bindings.put(binding.name, binding.item)
      case _ =>
        monitor ! GException(None,
          new PipelineException("badmessage", s"Unexpected message on $msg on $port", binding.location))
        return
    }
  }

  override protected def run(): Unit = {
    if (traceEnabled("Bindings")) {
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
      trace(s"CMPUT ${binding.name}=${binding.expression}$sbindings", "Bindings")
    }

    val msg = new BindingMessage(binding.name, binding.expression.get)
    monitor ! GOutput(binding, "result", msg)
    monitor ! GClose(binding, "result")
    monitor ! GFinished(binding)
  }
}
