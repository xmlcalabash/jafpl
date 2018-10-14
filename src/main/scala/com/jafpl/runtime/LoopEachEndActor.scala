package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.ContainerEnd

private[runtime] class LoopEachEndActor(private val monitor: ActorRef,
                                        override protected val runtime: GraphRuntime,
                                        override protected val node: ContainerEnd) extends LoopEndActor(monitor, runtime, node)  {

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [LoopEnd]"
  }
}
