package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.ContainerEnd

private[runtime] class LoopEachEndActor(private val monitor: ActorRef,
                                        private val runtime: GraphRuntime,
                                        private val node: ContainerEnd) extends LoopEndActor(monitor, runtime, node)  {
}
