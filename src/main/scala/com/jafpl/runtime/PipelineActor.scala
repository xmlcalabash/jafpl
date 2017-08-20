package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.ContainerStart

private[runtime] class PipelineActor(private val monitor: ActorRef,
                                     private val runtime: GraphRuntime,
                                     private val node: ContainerStart) extends StartActor(monitor, runtime, node) {
}
