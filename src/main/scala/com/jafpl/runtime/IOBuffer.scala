package com.jafpl.runtime

import com.jafpl.messages.Message
import com.jafpl.steps.DataConsumer

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[jafpl] class IOBuffer extends DataConsumer {
  private val buffer = mutable.HashMap.empty[String, ListBuffer[Message]]

  /** Send output from a step.
    *
    * Calling this method sends the specified `message` on the specified `port`.
    *
    * @param port    The output port.
    * @param message The message.
    */
  override def consume(port: String, message: Message): Unit = {
    val lb = buffer.getOrElse(port, ListBuffer.empty[Message])
    lb += message
    buffer(port) = lb
  }

  def reset(): Unit = {
    buffer.clear()
  }

  def reset(port: String): Unit = {
    buffer -= port
  }

  def ports: Set[String] = buffer.keySet.toSet

  def messages(port: String): List[Message] = {
    buffer(port).toList
  }

  def clear(port: String): Unit = {
    if (buffer.contains(port)) {
      buffer -= port
    }
  }
}
