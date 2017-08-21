package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.Node
import com.jafpl.runtime.GraphMonitor.{GClose, GOutput}
import com.jafpl.steps.{StepDataProvider, DataProvider}

class InputProxy(private val monitor: ActorRef,
                 private val runtime: GraphRuntime,
                 private val node: Node) extends DataProvider {
  var _closed = false

  def closed: Boolean = _closed

  def send(item: Any): Unit = {
    monitor ! GOutput(node, "result", item)
  }

  def close(): Unit = {
    _closed = true
  }
}
