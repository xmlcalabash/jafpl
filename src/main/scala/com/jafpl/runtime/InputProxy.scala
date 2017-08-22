package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.Node
import com.jafpl.runtime.GraphMonitor.{GClose, GOutput}
import com.jafpl.steps.{DataProvider, StepDataProvider}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class InputProxy(private val monitor: ActorRef,
                 private val runtime: GraphRuntime,
                 private val node: Node) extends DataProvider {
  var _closed = false
  val _items = mutable.ListBuffer.empty[Any]

  def closed: Boolean = _closed
  private[runtime] def items: ListBuffer[Any] = _items
  private[runtime] def clear() = {
    _items.clear()
  }

  def send(item: Any): Unit = {
    _items += item
  }

  def close(): Unit = {
    _closed = true
  }
}
