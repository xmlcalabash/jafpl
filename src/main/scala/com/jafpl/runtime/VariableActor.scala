package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.Binding
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class VariableActor(private val monitor: ActorRef,
                                     override protected val runtime: GraphRuntime,
                                     private val binding: Binding) extends AtomicActor(monitor, runtime, binding) {
  private val exprContext = ListBuffer.empty[Message]
  private val bindings = mutable.HashMap.empty[String, Message]
  logEvent = TraceEvent.VARIABLE

  override protected def input(port: String, item: Message): Unit = {
    item match {
      case item: ItemMessage =>
        assert(port == "source")
        exprContext += item
      case binding: BindingMessage =>
        assert(port == "#bindings")
        bindings.put(binding.name, binding.message)
      case _ =>
        throw JafplException.unexpectedMessage(item.toString, port, binding.location)
    }
  }

  override protected def run(): Unit = {
    // Pass any statics in as normal bindings
    for ((binding,message) <- node.staticBindings) {
      if (!bindings.contains(binding.name)) {
        bindings.put(binding.name, message)
      }
    }

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

    val expreval = runtime.runtime.expressionEvaluator.newInstance()
    val answer = expreval.value(binding.expression, exprContext.toList, bindings.toMap, binding.params)

    val msg = new BindingMessage(binding.name, answer)
    sendMessage("result", msg)
    super.run()
  }

  override protected def reset(): Unit = {
    super.reset()

    // Setup for the next loop
    exprContext.clear()
    bindings.clear()
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Variable]"
  }
}
