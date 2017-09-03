package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.{CatchStart, ContainerStart, Joiner, Node, Splitter, TryStart}
import com.jafpl.runtime.GraphMonitor.{GAbort, GCatch, GClose, GException, GFinished, GStart}

private[runtime] class TryCatchActor(private val monitor: ActorRef,
                                     private val runtime: GraphRuntime,
                                     private val node: ContainerStart) extends StartActor(monitor, runtime, node) {
  var runningTryBlock = true
  var tryblock: Option[TryStart] = None

  override protected def start(): Unit = {
    readyToRun = true
    runningTryBlock = true

    for (child <- node.children) {
      child match {
        case block: TryStart =>
          tryblock = Some(block)
          monitor ! GStart(block)
        case block: CatchStart =>
          Unit
        case _ =>
          monitor ! GStart(child)
      }
    }
  }

  def exception(cause: Throwable): Unit = {
    if (runningTryBlock) {
      runningTryBlock = false

      monitor ! GAbort(tryblock.get)

      var code: Option[String] = None
      cause match {
        case pe: PipelineException =>
          code = Some(pe.code)
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
              if (catchBlock.codes.isEmpty || (code.isDefined && catchBlock.codes.contains(code.get))) {
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
    trace(s"KILLC $node", "TryCatch")
    monitor ! GAbort(node)
    for (output <- node.outputs) {
      monitor ! GClose(node, output)
    }
  }

  override protected[runtime] def finished(): Unit = {
    trace(s"FINISH TRY/CATCH SUCCESSFULLY", "TryCatch")
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
  }
}
