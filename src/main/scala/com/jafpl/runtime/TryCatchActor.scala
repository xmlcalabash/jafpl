package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.JafplExceptionCode
import com.jafpl.graph.{CatchStart, FinallyStart, Node, NodeState, TryCatchStart, TryStart}
import com.jafpl.runtime.NodeActor.{NAbort, NAborted, NFinished, NReady, NRun}

import scala.collection.mutable.ListBuffer

private[runtime] class TryCatchActor(private val monitor: ActorRef,
                                      override protected val runtime: GraphRuntime,
                                      override protected val node: TryCatchStart) extends StartActor(monitor, runtime, node) {
  private var tryStart: TryStart = _
  private val catchList = ListBuffer.empty[CatchStart]
  private var finallyStart = Option.empty[FinallyStart]
  private var cause = Option.empty[Exception]
  private var aborting = false

  override protected def initialize(): Unit = {
    for (node <- node.children) {
      node match {
        case trys: TryStart =>
          tryStart = trys
        case ctch: CatchStart =>
          catchList += ctch
        case fin: FinallyStart =>
          finallyStart = Some(fin)
        case _ => ()
      }
    }
    super.initialize()
  }

  override protected def reset(): Unit = {
    aborting = false
    catchList.clear()
    cause = None
    for (node <- node.children) {
      node match {
        case ctch: CatchStart =>
          catchList += ctch
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
        case _: TryStart => ready = ready && cnode.state == NodeState.READY
        case _: CatchStart => ready = ready && cnode.state == NodeState.READY
        case _: FinallyStart => ready = ready && cnode.state == NodeState.READY
        case _ => ready = ready && (cnode.state == NodeState.READY || cnode.state == NodeState.RESET)
      }
    }
    if (ready) {
      parent ! NReady(node)
    }

    if (node.state == NodeState.RUNNING) {
      // TryCatch doesn't automatically run its when children, even if they're ready
      child match {
        case _: TryStart => ()
        case _: CatchStart => ()
        case _: FinallyStart => ()
        case _ =>
          if (child.state == NodeState.READY) {
            stateChange(child, NodeState.RUNNING)
            actors(child) ! NRun()
          }
      }
    }
  }

  override protected def run(): Unit = {
    for (cnode <- node.children) {
      cnode match {
        case _: TryStart => ()
        case _: CatchStart => ()
        case _: FinallyStart => ()
        case _ =>
          if (cnode.state == NodeState.READY) {
            stateChange(cnode, NodeState.RUNNING)
            actors(cnode) ! NRun()
          }
      }
    }

    stateChange(tryStart, NodeState.RUNNING)
    actors(tryStart) ! NRun()
  }

  override protected def exceptionHandler(child: Node, cause: Exception): Unit = {
    if (!child.isInstanceOf[TryStart]) {
      super.exceptionHandler(child, cause)
      return
    }

    var code: Option[Any] = None
    cause match {
      case je: JafplExceptionCode =>
        code = Some(je.jafplExceptionCode)
        trace("CAUGHTEX", s"Code=${code.get}; ${cause.getMessage}", logEvent)
      case _ =>
        trace("CAUGHTEX", s"${cause.getMessage}", logEvent)
    }

    var useCatch: Option[CatchStart] = None
    for (catchBlock <- catchList) {
      if (useCatch.isEmpty) {
        var codeMatches = catchBlock.codes.isEmpty
        if (code.isDefined) {
          for (catchCode <- catchBlock.codes) {
            codeMatches = codeMatches || (code.get == catchCode)
          }
        }
        if (codeMatches) {
          useCatch = Some(catchBlock)
        }
      }
    }

    if (useCatch.isDefined) {
      actors(child) ! NAbort()
      for (catchBlock <- catchList) {
        if (catchBlock != useCatch.get) {
          actors(catchBlock) ! NAbort()
        }
      }
      this.cause = Some(cause)
      useCatch.get.cause = this.cause
      stateChange(useCatch.get, NodeState.RUNNING)
      actors(useCatch.get) ! NRun()
    } else {
      super.exceptionHandler(child, cause)
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
    if (child.isInstanceOf[TryStart]) {
      for (ctch <- catchList) {
        actors(ctch) ! NAbort()
      }
    }

    var unfinishedCount = 0
    var unfinishedNode = Option.empty[Node]
    for (cnode <- node.children) {
      if (cnode.state == NodeState.FINISHED || cnode.state == NodeState.ABORTED) {
        // nop
      } else {
        unfinishedCount += 1
        unfinishedNode = Some(cnode)
      }
    }

    if (unfinishedCount == 0) {
      parent ! NFinished(node)
    } else {
      trace("UNFINISH", s"${nodeState(node)}", TraceEvent.STATECHANGE)
      if (unfinishedCount == 1) {
        unfinishedNode.get match {
          case fin: FinallyStart =>
            fin.cause = cause
            stateChange(fin, NodeState.RUNNING)
            actors(fin) ! NRun()
          case _ => ()
        }
      }
    }
  }
}
