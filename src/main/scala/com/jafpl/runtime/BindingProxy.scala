package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.Binding
import com.jafpl.runtime.GraphMonitor.GException
import com.jafpl.steps.BindingProvider

class BindingProxy(private val monitor: ActorRef,
                   private val runtime: GraphRuntime,
                   private val binding: Binding) extends BindingProvider {
  var _closed = false
  var _value = Option.empty[Any]

  def value = _value
  def closed: Boolean = _closed

  def set(item: Any): Unit = {
    if (closed) {
      monitor ! GException(None,
        new PipelineException("bindingclosed", s"Attempt to change closed binding: ${binding.name}", binding.location))
    } else {
      _value = Some(item)
      _closed = true
    }
  }
}
