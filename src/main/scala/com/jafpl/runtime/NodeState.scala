package com.jafpl.runtime

object NodeState extends Enumeration {
  type NodeState = Value
  val LIMBO, CREATED, RUNNABLE, READY, RUNNING, LOOPING, STOPPED, FINISHED, ABORTED = Value
}
