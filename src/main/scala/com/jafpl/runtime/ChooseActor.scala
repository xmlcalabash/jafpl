package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{ChooseStart, Node, NodeState, WhenStart}
import com.jafpl.runtime.NodeActor.{NAbort, NAborted, NFinished, NGuardCheck, NRunIfReady}

import scala.collection.mutable.ListBuffer

private[runtime] class ChooseActor(private val monitor: ActorRef,
                                    override protected val runtime: GraphRuntime,
                                    override protected val node: ChooseStart) extends StartActor(monitor, runtime, node) {
  private val whenList = ListBuffer.empty[WhenStart]
  private var aborting = false

  override protected def initialize(): Unit = {
    for (node <- node.children) {
      node match {
        case when: WhenStart =>
          whenList += when
        case _ => Unit
      }
    }
    super.initialize()
  }

  override protected def reset(): Unit = {
    aborting = false
    whenList.clear()
    for (node <- node.children) {
      node match {
        case when: WhenStart =>
          whenList += when
        case _ => Unit
      }
    }
    super.reset()
  }

  override protected def run(): Unit = {
    stateChange(node, NodeState.RUNNING)

    for (cnode <- node.children) {
      cnode match {
        case _: WhenStart => Unit
        case _ => actors(cnode) ! NRunIfReady()

      }
    }

    testNextCondition()
  }

  private def testNextCondition(): Unit = {
    if (whenList.isEmpty) {
      parent ! NFinished(node)
    } else {
      val when = whenList.head
      whenList -= when
      actors(when) ! NGuardCheck()
    }
  }

  def guardReport(when: WhenStart, pass: Boolean): Unit = {
    if (pass) {
      actors(when) ! NRunIfReady()
      for (branch <- whenList) {
        actors(branch) ! NAbort()
      }
      whenList.clear()
    } else {
      actors(when) ! NAbort()
      testNextCondition()
    }
  }

  override protected def abort(): Unit = {
    aborting = true
    super.abort()
  }

  override protected def aborted(child: Node): Unit = {
    stateChange(child, NodeState.ABORTED)
    var aborted = true
    for (cnode <- node.children) {
      aborted = aborted && cnode.state == NodeState.ABORTED
    }

    if (aborting) {
      if (aborted) {
        parent ! NAborted(node)
      }
    } else {
      var finished = true
      for (cnode <- node.children) {
        finished = finished && (cnode.state == NodeState.FINISHED || cnode.state == NodeState.ABORTED)
      }
      if (finished) {
        parent ! NFinished(node)
      } else {
        trace("UNFINISH", s"${nodeState(node)}", TraceEvent.STATECHANGE)
      }
    }
  }

  override protected def finished(child: Node): Unit = {
    stateChange(child, NodeState.FINISHED)
    var finished = true
    for (cnode <- node.children) {
      finished = finished && (cnode.state == NodeState.FINISHED || cnode.state == NodeState.ABORTED)
    }
    if (finished) {
      parent ! NFinished(node)
    } else {
      trace("UNFINISH", s"${nodeState(node)}", TraceEvent.STATECHANGE)
    }
  }
}
