package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.GraphException
import com.jafpl.graph.Binding
import com.jafpl.messages.BindingMessage
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput}

import scala.collection.mutable

private[runtime] class VariableActor(private val monitor: ActorRef,
                                     private val runtime: GraphRuntime,
                                     private val binding: Binding)
  extends NodeActor(monitor, runtime, binding)  {
  var exprContext = Option.empty[Any]
  val bindings = mutable.HashMap.empty[String, Any]

  override protected def input(port: String, item: Any): Unit = {
    if (port == "source") {
      trace(s"BCNXT $item", "Bindings")
      exprContext = Some(item)
    } else {
      item match {
        case msg: BindingMessage =>
          trace(s"BOUND ${msg.name}=${msg.item}", "Bindings")
          bindings.put(msg.name, msg.item)
        case _ => throw new GraphException(s"Unexpected message on $port")
      }
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
