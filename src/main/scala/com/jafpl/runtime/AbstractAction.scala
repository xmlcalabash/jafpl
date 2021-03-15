package com.jafpl.runtime

import com.jafpl.graph.Node
import com.jafpl.messages.{BindingMessage, Message}
import com.jafpl.runtime.NodeState.NodeState
import com.jafpl.steps.DataConsumer
import com.jafpl.util.TraceEventManager

import scala.collection.mutable

abstract class AbstractAction(val node: Node) extends Action with DataConsumer {
  protected var scheduler: Scheduler = _
  protected var tracer: TraceEventManager = _
  protected var receivedBindings = mutable.HashMap.empty[String, Message]

  override def initialize(scheduler: Scheduler, node: Node): Unit = {
    this.scheduler = scheduler
    tracer = scheduler.runtime.traceEventManager
    tracer.trace("INIT  " + this + " for " + node, TraceEventManager.INIT)
    if (node.step.isDefined) {
      node.step.get.initialize(scheduler.runtime.runtime)
    }
  }

  override def start(): Unit = {
    tracer.trace("START  " + this + " for " + node, TraceEventManager.START)
  }

  override def receive(port: String, message: Message): Unit = {
    tracer.trace("RECV  " + this + " for " + port + ": " + message, TraceEventManager.RECEIVE)
  }

  override def close(port: String): Unit = {
    tracer.trace("CLOS? " + this + " for " + port, TraceEventManager.RECEIVE)
  }

  // Why not BindingMessage?
  override def receiveBinding(message: Message): Unit = {
    message match {
      case msg: BindingMessage =>
        tracer.trace("RECVB " + this + " for " + msg.name, TraceEventManager.BINDING)
        receivedBindings.put(msg.name, msg.message)
      case _ =>
        tracer.trace("RECVB " + this + " for ???: " + message, TraceEventManager.BINDING)
    }
  }

  override def run(): Unit = {
    tracer.trace(s"RUN   $this ************************************************************", TraceEventManager.RUN)
  }

  override def reset(state: NodeState): Unit = {
    tracer.trace(s"RESET $this to $state", TraceEventManager.RESET)
    receivedBindings.clear()
  }

  override def abort(): Unit = {
    tracer.trace("ABORT " + this, TraceEventManager.ABORT)
    receivedBindings.clear()
  }

  override def stop(): Unit = {
    tracer.trace("STOP " + this, TraceEventManager.STOP)
  }

  override def toString: String = {
    node.toString + ".Action"
  }

  /** Send output from a step.
   *
   * Calling this method sends the specified `message` on the specified `port`.
   *
   * @param port    The output port.
   * @param message The message.
   */
  override def consume(port: String, message: Message): Unit = {
    scheduler.receive(this, port, message)
  }
}
