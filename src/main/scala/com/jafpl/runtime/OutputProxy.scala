package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.Node
import com.jafpl.messages.Metadata
import com.jafpl.runtime.GraphMonitor.GException
import com.jafpl.steps.{DataConsumer, DataProvider}

class OutputProxy(private val monitor: ActorRef,
                  private val runtime: GraphRuntime,
                  private val node: Node) extends DataConsumer  {
  private var _provider = Option.empty[DataProvider]

  def provider: Option[DataProvider] = _provider

  override def setProvider(provider: DataProvider): Unit = {
    if (_provider.isDefined) {
      monitor ! GException(None,
        new PipelineException("dupprovider", "Attempt to reset provider.", node.location))
    }
    _provider = Some(provider)
  }

  override def send(port: String, item: Any, metadata: Metadata): Unit = {
    if (_provider.isDefined) {
      _provider.get.send(item, metadata)
    }
  }
}
