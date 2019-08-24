package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.Node
import com.jafpl.messages.Message
import com.jafpl.steps.DataConsumer

private[runtime] class ConsumingProxy(private val monitor: ActorRef,
                                       private val runtime: GraphRuntime,
                                       private val node: Node) extends DataConsumer {
  override def consume(port: String, message: Message): Unit = {
    println(s"$node/$port consumes $message")
  }
}
