package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.{Binding, Node}
import com.jafpl.messages.BindingMessage
import com.jafpl.runtime.GraphMonitor.{GClose, GFinished, GOutput}
import com.jafpl.steps.{BindingProvider, DataProvider}

class BindingProxy(private val monitor: ActorRef,
                   private val runtime: GraphRuntime,
                   private val binding: Binding) extends BindingProvider {
  var _closed = false
  var _value = Option.empty[Any]

  def value = _value
  def closed: Boolean = _closed

  def set(item: Any): Unit = {
    if (closed) {
      throw new PipelineException("bindclosed", "Attempt to change closed binding for " + binding.name)
    }
    _value = Some(item)
    _closed = true
  }
}
