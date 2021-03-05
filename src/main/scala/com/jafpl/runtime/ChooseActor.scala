package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{ChooseStart, Node, NodeState, WhenStart}
import com.jafpl.runtime.NodeActor.{NAbort, NAborted, NFinished, NGuardCheck, NReady, NRun}

import scala.collection.mutable.ListBuffer

private[runtime] class ChooseActor(private val monitor: ActorRef,
                                    override protected val runtime: GraphRuntime,
                                    override protected val node: ChooseStart) extends StartActor(monitor, runtime, node) {
  private val whenList = ListBuffer.empty[WhenStart]
  private var nextWhen = Option.empty[WhenStart]
  private var aborting = false

  override protected def initialize(): Unit = {
    for (node <- node.children) {
      node match {
        case when: WhenStart =>
          whenList += when
        case _ => ()
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
        case _ => ()
      }
    }
    super.reset()
  }

  override protected def ready(child: Node): Unit = {
    stateChange(child, NodeState.READY)
    var ready = true
    for (cnode <- node.children) {
      cnode match {
        case _: WhenStart => ready = ready && cnode.state == NodeState.READY
        case _ => ready = ready && (cnode.state == NodeState.READY || cnode.state == NodeState.RESET)
      }
    }

    if (node.state == NodeState.RUNNING) {
      // Choose doesn't automatically run its when children, even if they're ready
      child match {
        case when: WhenStart =>
          if (nextWhen.isDefined && nextWhen.get == when) {
            nextWhen = None
            actors(when) ! NGuardCheck()
          }
        case _ =>
          if (child.state == NodeState.READY) {
            stateChange(child, NodeState.RUNNING)
            actors(child) ! NRun()
          }
      }
    } else {
      if (ready) {
        parent ! NReady(node)
      }
    }
  }

  override protected def run(): Unit = {
    for (cnode <- node.children) {
      cnode match {
        case _: WhenStart => ()
        case _ =>
          if (cnode.state == NodeState.READY) {
            stateChange(cnode, NodeState.RUNNING)
            actors(cnode) ! NRun()
          }
      }
    }

    testNextCondition()
  }

  private def testNextCondition(): Unit = {
    if (whenList.isEmpty) {
      // We must have aborted everyone...we'll end when they're all aborted
    } else {
      val when = whenList.head
      whenList -= when
      if (when.state == NodeState.READY) {
        actors(when) ! NGuardCheck()
      } else {
        nextWhen = Some(when)
      }
    }
  }

  def guardReport(when: WhenStart, pass: Boolean): Unit = {
    if (pass) {
      stateChange(when, NodeState.RUNNING)
      actors(when) ! NRun()
      for (branch <- whenList) {
        actors(branch) ! NAbort()
      }
      whenList.clear()
    } else {
      stateChange(when, NodeState.ABORTING)
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
