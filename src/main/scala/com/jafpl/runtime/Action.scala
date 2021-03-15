package com.jafpl.runtime

import com.jafpl.graph.Node
import com.jafpl.messages.Message
import com.jafpl.runtime.NodeState.NodeState

trait Action extends Runnable {
  def node: Node

  def initialize(scheduler: Scheduler, node: Node): Unit

  def start(): Unit

  def receive(port: String, message: Message): Unit

  def close(port: String): Unit

  def receiveBinding(message: Message): Unit

  def run(): Unit

  def reset(state: NodeState): Unit

  def abort(): Unit

  def stop(): Unit
}
