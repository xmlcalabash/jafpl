package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.exceptions.{JafplException, JafplExceptionCode}
import com.jafpl.graph.{CatchStart, ContainerStart, FinallyStart, Joiner, Node, Sink, Splitter, TryStart}
import com.jafpl.messages.{BindingMessage, Message}
import com.jafpl.runtime.GraphMonitor.{GAbort, GCatch, GCheckGuard, GClose, GException, GFinished, GOutput, GRunFinally, GStart}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

private[runtime] class TryCatchActor(private val monitor: ActorRef,
                                     override protected val runtime: GraphRuntime,
                                     override protected val node: ContainerStart) extends StartActor(monitor, runtime, node) {
  var tryblock: Option[TryStart] = None
  var finblock: Option[FinallyStart] = None
  val catchList = ListBuffer.empty[CatchStart]
  var cause = Option.empty[Throwable]
  val buffers = mutable.HashMap.empty[String,ListBuffer[Message]]
  logEvent = TraceEvent.TRY

  override protected def input(from: Node, fromPort: String, port: String, item: Message): Unit = {
    trace("INPUT", s"$node $from.$fromPort to $port", logEvent)

    if (openOutputs.contains(port)) {
      trace("BUFFER", s"$node.$port -> $item", logEvent)
      node.outputCardinalities.put(port, node.outputCardinalities.getOrElse(port, 0L) + 1)
      if (buffers.contains(port)) {
        buffers(port) += item
      } else {
        val lb = ListBuffer.empty[Message]
        lb += item
        buffers(port) = lb
      }
      return
    }

    // Bindings on compound steps are only ever for injectables
    if (port == "#bindings") {
      for (inj <- node.stepInjectables) {
        item match {
          case binding: BindingMessage =>
            inj.receiveBinding(binding)
          case _ =>
            throw new RuntimeException("FIXME: bang")
        }
      }
    } else {
      consume(port,item)
    }
  }

  override protected def reset(): Unit = {
    tryblock = None
    finblock = None
    catchList.clear()
    cause = None
    super.reset()
  }

  override protected def run(): Unit = {
    trace("RUN", s"$node", logEvent)

    for (child <- node.children) {
      childState(child) = NodeState.STARTED
    }
    trace("CSTATE", s"$node / $childState", logEvent)

    for (child <- node.children) {
      child match {
        case join: Joiner =>
          monitor ! GStart(join)
        case split: Splitter =>
          monitor ! GStart(split)
        case sink: Sink =>
          monitor ! GStart(sink)
        case block: TryStart =>
          tryblock = Some(block)
        case block: CatchStart =>
          catchList += block
        case block: FinallyStart =>
          finblock = Some(block)
      }
    }

    monitor ! GStart(tryblock.get)
  }

  override protected[runtime] def finished(): Unit = {
    for (port <- buffers.keySet) {
      for (item <- buffers(port)) {
        monitor ! GOutput(node, port, item)
      }
    }
    super.finished()
  }

  override protected[runtime] def childFinished(child: Node): Unit = {
    trace("CFINISH", s"$node/$child: $childState : $openOutputs", logEvent)
    if (child == tryblock.get) {
      if (tryblock.get.state == NodeState.ABORTED) {
        node.state = NodeState.ABORTED
        handleException()
      } else {
        for (catchBlock <- catchList) {
          stopUnselectedBranch(catchBlock)
        }
      }
      childState(child) = NodeState.FINISHED
      runFinally()
      finishIfReady()
    } else {
      super.childFinished(child)
    }
  }

  def runFinally(): Unit = {
    if (finblock.isDefined) {
      trace("RUNFINAL", s"$node", logEvent)
      monitor ! GRunFinally(finblock.get, cause)
    }
  }

  private def handleException(): Unit = {
    trace("HANDLEX", s"$node: cause: ${cause.isDefined} state: ${node.state}", logEvent)
    if (cause.isEmpty || node.state != NodeState.ABORTED) {
      return
    }

    buffers.clear()

    var code: Option[Any] = None
    cause.get match {
      case je: JafplExceptionCode =>
        code = Some(je.jafplExceptionCode)
        trace("EXCODE", s"Exception code=${code.get}", logEvent)
      case _ => Unit
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
          trace("CAUGHT", s"$catchBlock", logEvent)
          useCatch = Some(catchBlock)
        } else {
          stopUnselectedBranch(catchBlock)
        }
      } else {
        stopUnselectedBranch(catchBlock)
      }
    }

    runFinally()

    if (useCatch.isDefined) {
      monitor ! GCatch(useCatch.get, cause.get)
    } else {
      // Not my problem
      monitor ! GException(node.parent, cause.get)
    }
  }

  def exception(cause: Throwable): Unit = {
    trace("EXCEPTION", s"$node $cause", logEvent)
    this.cause = Some(cause)
    handleException()
  }

  private def stopUnselectedBranch(node: Node): Unit = {
    trace("KILLCATCH", s"${this.node} $node", logEvent)
    node.state = NodeState.ABORTED
    childState(node) = NodeState.ABORTED
    monitor ! GAbort(node)
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [TryCatch]"
  }
}
