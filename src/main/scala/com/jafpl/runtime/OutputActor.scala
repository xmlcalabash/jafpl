package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.Node

private[runtime] class OutputActor(private val monitor: ActorRef,
                                   private val runtime: GraphRuntime,
                                   private val node: Node,
                                   private val consumer: OutputProxy)
  extends NodeActor(monitor, runtime, node, consumer) {
  private var closed = false

  override protected def input(port: String, item: Any): Unit = {
    if (consumer.provider.isDefined) {
      trace(s"??CNSM $item", "Consumer")
      consumer.provider.get.send(item)
    } else {
      trace(s"!!CNSM $item", "Consumer")
    }
  }

  override protected def close(port: String): Unit = {
    if (consumer.provider.isDefined) {
      trace(s"??CLOS", "Consumer")
      consumer.provider.get.close()
    }
    closed = true
    runIfReady()
  }

  override protected def start(): Unit = {
    readyToRun = true
    runIfReady()
  }

  private def runIfReady(): Unit = {
    trace(s"RNIFR $node $readyToRun ${closed}", "StepExec")

    if (readyToRun && closed) {
      run()
    }
  }
}
