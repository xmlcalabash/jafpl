package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.ContainerStart

private[runtime] class PipelineActor(private val monitor: ActorRef,
                                     override protected val runtime: GraphRuntime,
                                     override protected val node: ContainerStart) extends StartActor(monitor, runtime, node) {

}
