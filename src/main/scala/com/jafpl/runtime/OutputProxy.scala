package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.Node
import com.jafpl.messages.Message
import com.jafpl.runtime.NodeActor.NException
import com.jafpl.steps.{DataConsumer, DataConsumerProxy}

class OutputProxy(private val monitor: ActorRef,
                  private val runtime: GraphRuntime,
                  private val node: Node) extends DataConsumerProxy with DataConsumer  {
  private var _provider = Option.empty[DataConsumer]

  def provider: Option[DataConsumer] = _provider

  override def setConsumer(provider: DataConsumer): Unit = {
    if (_provider.isDefined) {
      monitor ! NException(node,
        JafplException.internalError(s"Attempt to reset provider: $node", node.location))
    }
    _provider = Some(provider)
  }

  override def consume(port: String, message: Message): Unit = {
    if (_provider.isDefined) {
      _provider.get.consume("source", message)
    }
  }
}
