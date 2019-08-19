package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{GraphInput, Node}
import com.jafpl.messages.{Message, Metadata, PipelineMessage}
import com.jafpl.steps.{DataConsumer, DataProvider}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class InputProxy(private val monitor: ActorRef,
                 private val runtime: GraphRuntime,
                 private val node: Node) extends DataConsumer with DataProvider {
  private var _closed = false
  private val _items = mutable.ListBuffer.empty[Message]

  def closed: Boolean = _closed
  private[runtime] def items: ListBuffer[Message] = _items
  private[runtime] def clear(): Unit = {
    _items.clear()
  }

  override def send(message: Message): Unit = {
    // In fact the port name is irrelevant in the input proxy case...which is the whole point of send()!
    consume("source", message)
  }

  override def consume(port: String, message: Message): Unit = {
    _items += message
  }

  def close(): Unit = {
    // It's tempting to check input cardinality here; but it's unreliable.
    // What if the pipeline has some sort of default input mechanism? Then
    // we'll get 0 here even if there are in fact "inputs" to the pipeline.
    _closed = true
  }
}
