package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.TryStart

private[runtime] class TryActor(private val monitor: ActorRef,
                                 override protected val runtime: GraphRuntime,
                                 override protected val node: TryStart) extends StartActor(monitor, runtime, node) {

}
