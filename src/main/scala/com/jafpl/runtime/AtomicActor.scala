package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.AtomicNode
import com.jafpl.messages.{BindingMessage, ItemMessage, Message}
import com.jafpl.runtime.NodeActor.{NFinished, NReady, NStarted}

private[runtime] class AtomicActor(private val monitor: ActorRef,
                                    override protected val runtime: GraphRuntime,
                                    override protected val node: AtomicNode) extends NodeActor(monitor, runtime, node) {
  override protected def initialize(): Unit = {
    if (node.step.isDefined) {
      node.step.get.initialize(runtime.runtime)
    }
    super.initialize()
  }

  override protected def input(port: String, message: Message): Unit = {
    port match {
      case "#bindings" =>
        message match {
          case binding: BindingMessage =>
            if (node.step.isDefined) {
              trace("BINDING→", s"$node: ${binding.name}=${binding.message}", TraceEvent.BINDINGS)
              node.step.get.receiveBinding(binding)
            } else {
              trace("BINDING↴", s"$node: ${binding.name}=${binding.message}", TraceEvent.BINDINGS)
            }
          case _ =>
            throw JafplException.unexpectedMessage(message.toString, "#bindings", node.location)
        }
      case _ =>
        message match {
          case _: ItemMessage =>
            if (node.step.isDefined) {
              trace("DELIVER→", s"$node $port", TraceEvent.STEPIO)
              node.step.get.consume(port, message)
            } else {
              trace("DELIVER↴", s"$node $port", TraceEvent.STEPIO)
            }
          case _ =>
            throw JafplException.unexpectedMessage(message.toString, port, node.location)
        }
    }
  }

  override protected def close(port: String): Unit = {
    if (port == "#bindings") {
      for (port <- inputBuffer.ports) {
        for (message <- inputBuffer.messages(port)) {
          node.inputCardinalities.put(port, node.inputCardinalities.getOrElse(port, 0L) + 1)
          if (node.step.isDefined) {
            trace("DELIVER→", s"$node $port", TraceEvent.STEPIO)
            node.step.get.consume(port, message)
          } else {
            trace("DELIVER↴", s"$node $port", TraceEvent.STEPIO)
          }
        }
      }
    }

    super.close(port)
  }

  override protected def start(): Unit = {
    parent ! NStarted(node)
    if (openInputs.isEmpty) {
      parent ! NReady(node)
    }
  }

  override protected def run(): Unit = {
    if (node.step.isDefined) {
      node.step.get.run()
    }
    for (outputport <- outputBuffer.ports) {
      for (item <- outputBuffer.messages(outputport)) {
        sendMessage(outputport, item)
      }
    }
    closeOutputs()
    parent ! NFinished(node)
  }

  override protected def reset(): Unit = {
    super.reset()
    if (node.step.isDefined) {
      node.step.get.reset()
    }
  }
}
