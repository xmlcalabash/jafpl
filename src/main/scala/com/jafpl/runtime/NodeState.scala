package com.jafpl.runtime

object NodeState extends Enumeration {
  type NodeState = Value
  val CREATED, INIT, STARTED, CHECKGUARD, RESTARTING, FINISHED, STOPPED, ABORTED, RESETTING, RESET = Value
}
