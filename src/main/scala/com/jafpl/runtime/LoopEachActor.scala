package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{LoopEachStart, Node}
import com.jafpl.messages.{ItemMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GClose, GException, GFinished, GOutput, GRestartLoop}
import com.jafpl.steps.Manifold

import scala.collection.mutable.ListBuffer

private[runtime] class LoopEachActor(private val monitor: ActorRef,
                                     override protected val runtime: GraphRuntime,
                                     override protected val node: LoopEachStart)
  extends LoopActor(monitor, runtime, node) {

  private val queue = ListBuffer.empty[ItemMessage]
  logEvent = TraceEvent.LOOPEACH
  node.iterationPosition = 0L
  node.iterationSize = 0L

  // override protected def close(port: String): Unit = {

  // override protected def start(): Unit = {

  override protected[runtime] def finished(): Unit = {
    if (queue.isEmpty) {
      trace("LOOPFIN", s"$node", logEvent)
      checkCardinalities("current")

      // now close the outputs
      for (output <- node.outputs) {
        // Don't close 'current'; it must have been closed to get here and re-closing
        // it propagates the close event to the steps and they shouldn't see any more
        // events!
        if (output != "current") {
          trace("XYZ", s"$node 1 $output", logEvent)
          monitor ! GClose(node, output)
        }
      }

      if (childrenHaveFinished) {
        node.state = NodeState.FINISHED
        monitor ! GFinished(node)
        for (inj <- node.stepInjectables) {
          inj.afterRun()
        }
      }
    } else {
      trace("ITERFIN", s"$node children:$childrenHaveFinished", logEvent)
      if (childrenHaveFinished) {
        monitor ! GRestartLoop(node)
      }
    }
  }

  // override protected def abort(): Unit = {

  // override protected def stop(): Unit = {

  override protected def reset(): Unit = {
    running = false
    queue.clear()
    node.iterationPosition = 0L
    node.iterationSize = 0L
    super.reset()
  }

  // protected def runIfReady(): Unit = {

  override protected def run(): Unit = {
    trace("RUN", s"$node running:$running ready:$started", logEvent)

    running = true
    node.state = NodeState.STARTED

    if (node.iterationSize == 0) {
      node.iterationSize = queue.size
      if (queue.nonEmpty) {
        for (inj <- node.stepInjectables) {
          inj.beforeRun()
        }

        val port = "source"
        val count = node.inputCardinalities.getOrElse(port, 0L)
        val ispec = node.manifold.getOrElse(Manifold.ALLOW_ANY)
        trace("CARD", s"$node input $port: $count", TraceEvent.CARDINALITY)
        ispec.inputSpec.checkInputCardinality(port, count)
      }
    }

    if (queue.nonEmpty) {
      val item = queue.head
      queue -= item
      node.iterationPosition += 1

      val edge = node.outputEdge("current")
      monitor ! GOutput(node, edge.fromPort, item)
      trace("XYZ", s"$node 2 ${edge.fromPort}", logEvent)
      monitor ! GClose(node, edge.fromPort)
      super.run()
    } else {
      for (port <- openOutputs) {
        trace("XYZ", s"$node 3 $port", logEvent)
        monitor ! GClose(node, port)
      }
      openOutputs.clear()

      finishIfReady()
    }
  }

  override def consume(port: String, item: Message): Unit = {
    trace("RECEIVE", s"$node $port", logEvent)
    item match {
      case message: ItemMessage =>
        queue += message
        node.inputCardinalities.put(port, node.inputCardinalities.getOrElse(port, 0L) + 1)
      case _ =>
        monitor ! GException(Some(node), JafplException.unexpectedMessage(item.toString, port, node.location))
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [LoopEach]"
  }
}
