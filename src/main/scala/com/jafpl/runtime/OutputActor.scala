package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.Node
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.GException

private[runtime] class OutputActor(private val monitor: ActorRef,
                                   private val runtime: GraphRuntime,
                                   private val node: Node,
                                   private val consumer: OutputProxy)
  extends NodeActor(monitor, runtime, node, consumer) {
  private var closed = false

  override protected def input(port: String, msg: Message): Unit = {
    msg match {
      case item: ItemMessage =>
        if (consumer.provider.isDefined) {
          trace(s"??CNSM $item", "Consumer")
          runtime.dynamicContext.deliver(item, consumer.provider.get, port)
        } else {
          trace(s"!!CNSM $item", "Consumer")
        }
      case _ =>
        monitor ! GException(None,
          new PipelineException("badmessage", "Unexpected message $msg on $port", node.location))
    }
  }

  override protected def close(port: String): Unit = {
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
