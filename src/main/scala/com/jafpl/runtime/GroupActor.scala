package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.GroupStart

private[runtime] class GroupActor(private val monitor: ActorRef,
                                   override protected val runtime: GraphRuntime,
                                   override protected val node: GroupStart) extends StartActor(monitor, runtime, node) {

}
