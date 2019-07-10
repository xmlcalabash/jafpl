package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.{JafplException, JafplExceptionCode}
import com.jafpl.graph.{CatchStart, ContainerStart, FinallyStart, Joiner, Node, Splitter, TryStart}
import com.jafpl.runtime.GraphMonitor.{GAbort, GCatch, GClose, GException, GFinished, GRunFinally, GStart}

private[runtime] class TryCatchActor(private val monitor: ActorRef,
                                     override protected val runtime: GraphRuntime,
                                     override protected val node: ContainerStart) extends StartActor(monitor, runtime, node) {
  var runningTryBlock = true
  var tryblock: Option[TryStart] = None
  var finblock: Option[FinallyStart] = None
  var cause = Option.empty[Throwable]
  logEvent = TraceEvent.TRY

  override protected def start(): Unit = {
    trace("START", s"$node", logEvent)
    commonStart()
    runningTryBlock = true

    for (child <- node.children) {
      child match {
        case block: TryStart =>
          tryblock = Some(block)
          monitor ! GStart(block)
        case block: CatchStart =>
          Unit
        case block: FinallyStart =>
          finblock = Some(block)
          monitor ! GStart(block)
        case _ =>
          monitor ! GStart(child)
      }
    }
  }

  def runFinally(): Unit = {
    trace("RUNFINAL", s"$node", logEvent)
    monitor ! GRunFinally(finblock.get, cause)
  }

  def exception(cause: Throwable): Unit = {
    trace("EXCEPTION", s"$node $cause", logEvent)
    this.cause = Some(cause)

    if (runningTryBlock) {
      runningTryBlock = false

      monitor ! GAbort(tryblock.get)

      var code: Option[Any] = None
      cause match {
        case je: JafplExceptionCode =>
          code = Some(je.jafplExceptionCode)
        case _ => Unit
      }

      var useCatch: Option[CatchStart] = None
      for (child <- node.children) {
        child match {
          case tryBlock: TryStart =>
            stopUnselectedBranch(tryBlock)
          case catchBlock: CatchStart =>
            if (useCatch.isDefined) {
              stopUnselectedBranch(catchBlock)
            } else {
              var codeMatches = catchBlock.codes.isEmpty
              if (code.isDefined) {
                for (catchCode <- catchBlock.codes) {
                  codeMatches = codeMatches || (code.get == catchCode)
                }
              }

              if (codeMatches) {
                useCatch = Some(catchBlock)
              } else {
                stopUnselectedBranch(catchBlock)
              }
            }
          case _ => Unit
        }
      }

      if (useCatch.isDefined) {
        monitor ! GCatch(useCatch.get, cause)
      } else {
        // Not my problem
        monitor ! GException(node.parent, cause)
      }
    } else {
      // Not my problem
      monitor ! GException(node.parent, cause)
    }
  }

  private def stopUnselectedBranch(node: Node): Unit = {
    trace("KILLBRANCH", s"${this.node} $node", logEvent)
    monitor ! GAbort(node)
    for (output <- node.outputs) {
      monitor ! GClose(node, output)
    }
  }

  override protected[runtime] def finished(): Unit = {
    trace("FINISHED", s"$node finished successfully", logEvent)
    if (runningTryBlock) {
      // Yay, we succeeded, stop all the catch blocks
      for (child <- node.children) {
        child match {
          case catchBlock: CatchStart =>
            stopUnselectedBranch(catchBlock)
          case _ => Unit
        }
      }
    }

    monitor ! GFinished(node)
    commonFinished()
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [TryCatch]"
  }
}
