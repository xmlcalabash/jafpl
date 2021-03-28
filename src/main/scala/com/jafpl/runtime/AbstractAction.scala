package com.jafpl.runtime

import com.jafpl.graph.Node
import com.jafpl.messages.{BindingMessage, Message}
import com.jafpl.runtime.NodeState.NodeState
import com.jafpl.steps.DataConsumer
import com.jafpl.util.TraceEventManager

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

abstract class AbstractAction(val node: Node) extends Action with DataConsumer {
  protected var scheduler: Scheduler = _
  protected var tracer: TraceEventManager = _
  protected var receivedBindings = mutable.HashMap.empty[String, Message]
  private val _received = mutable.HashMap.empty[String, ListBuffer[Message]]

  def receivedPorts: List[String] = _received.keys.toList
  def received(port: String): List[Message] = {
    var messages = List[Message]()
    if (_received.contains(port)) {
      messages = _received(port).toList
      _received.remove(port)
    }
    messages
  }

  override def initialize(scheduler: Scheduler, node: Node): Unit = {
    this.scheduler = scheduler
    tracer = scheduler.runtime.traceEventManager
    tracer.trace("INIT  " + this + " for " + node, TraceEventManager.INIT)
    if (node.step.isDefined) {
      node.step.get.initialize(scheduler.runtime.runtime)
    }
  }

  override def start(): Unit = {
    tracer.trace(s"START  $this for $node", TraceEventManager.START)
  }

  override def receive(port: String, message: Message): Unit = {
    message match {
      case msg: BindingMessage =>
        tracer.trace(s"RECVB $this for ${msg.name}", TraceEventManager.BINDING)
        receivedBindings.put(msg.name, msg.message)
      case _ =>
        tracer.trace(s"RECV  $this for $port: $message", TraceEventManager.RECEIVE)
        if (node.inputs.contains(port) || port.startsWith("#")) {
          if (_received.contains(port)) {
            _received(port) += message
          } else {
            _received.put(port, ListBuffer(message))
          }
        } else {
          tracer.trace("error", s"INTERNAL ERROR: Ignoring input for unexpected port $this: $port ($message)", TraceEventManager.MESSAGES)
        }
    }
  }

  // A convenience for subtypes
  def receiveBinding(message: BindingMessage): Unit = {
    tracer.trace(s"RECVB $this for ${message.name}", TraceEventManager.BINDING)
    receivedBindings.put(message.name, message)
  }

  override def close(port: String): Unit = {
    tracer.trace("CLOS? " + this + " for " + port, TraceEventManager.RECEIVE)
  }

  override def run(): Unit = {
    tracer.trace(s"RUN   $this ************************************************************", TraceEventManager.RUN)
  }

  def cleanup(): Unit = {
    receivedBindings.clear()
    _received.clear()
  }

  override def reset(state: NodeState): Unit = {
    tracer.trace(s"RESET $this to $state", TraceEventManager.RESET)
    if (node.step.isDefined) {
      node.step.get.reset()
    }
    cleanup()
  }

  override def abort(): Unit = {
    tracer.trace("ABORT " + this, TraceEventManager.ABORT)
    if (node.step.isDefined) {
      node.step.get.abort()
    }
    cleanup()
  }

  override def stop(): Unit = {
    tracer.trace("STOP " + this, TraceEventManager.STOP)
    if (node.step.isDefined) {
      node.step.get.stop()
    }
    cleanup()
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
