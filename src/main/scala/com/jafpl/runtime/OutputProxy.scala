package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.Node
import com.jafpl.messages.Metadata
import com.jafpl.runtime.GraphMonitor.GException
import com.jafpl.steps.{DataConsumerProxy, DataConsumer}

class OutputProxy(private val monitor: ActorRef,
                  private val runtime: GraphRuntime,
                  private val node: Node) extends DataConsumerProxy with DataConsumer  {
  private var _provider = Option.empty[DataConsumer]

  def provider: Option[DataConsumer] = _provider

  override def setConsumer(provider: DataConsumer): Unit = {
    if (_provider.isDefined) {
      monitor ! GException(None,
        new PipelineException("dupprovider", "Attempt to reset provider.", node.location))
    }
    _provider = Some(provider)
  }

  override def receive(port: String, item: Any, metadata: Metadata): Unit = {
    if (_provider.isDefined) {
      _provider.get.receive("source", item, metadata)
    }
  }
}
