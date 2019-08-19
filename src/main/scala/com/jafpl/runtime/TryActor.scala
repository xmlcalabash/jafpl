package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplExceptionCode
import com.jafpl.graph.{CatchStart, ContainerStart, FinallyStart, Joiner, Node, Sink, Splitter, TryStart}
import com.jafpl.messages.{BindingMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GAbort, GCatch, GClose, GException, GOutput, GRunFinally, GStart}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class TryActor(private val monitor: ActorRef,
                                override protected val runtime: GraphRuntime,
                                override protected val node: ContainerStart) extends StartActor(monitor, runtime, node) {

  def exception(cause: Throwable): Unit = {
    node.state = NodeState.ABORTED
    monitor ! GException(node.parent, cause)
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Try]"
  }
}
