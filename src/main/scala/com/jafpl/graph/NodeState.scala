package com.jafpl.graph

object NodeState extends Enumeration {
  type NodeState = Value
  val CREATED, INIT, STARTED, STARTING, READY, RUNNING, CHECKGUARD, RESTARTING, FINISHED, STOPPING, STOPPED, ABORTING, ABORTED, LOOPING, RESETTING, RESET = Value
}
