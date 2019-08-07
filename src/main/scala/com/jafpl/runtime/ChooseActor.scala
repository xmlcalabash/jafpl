package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{ContainerStart, Joiner, Node, Sink, Splitter, WhenStart}
import com.jafpl.runtime.GraphMonitor.{GAbort, GCheckGuard, GClose, GStart}

import scala.collection.mutable.ListBuffer

private[runtime] class ChooseActor(private val monitor: ActorRef,
                                   override protected val runtime: GraphRuntime,
                                   override protected val node: ContainerStart) extends StartActor(monitor, runtime, node) {
  val whenList = ListBuffer.empty[Node]
  logEvent = TraceEvent.CHOOSE

  override protected def start(): Unit = {
    trace("START", s"$node", logEvent)

    commonStart()

    for (child <- node.children) {
      child match {
        case join: Joiner =>
          monitor ! GStart(join)
        case split: Splitter =>
          monitor ! GStart(split)
        case sink: Sink =>
          monitor ! GStart(sink)
        case _ =>
          whenList += child
      }
    }

    if (whenList.nonEmpty) {
      val nextWhen = whenList.head
      whenList.remove(0)
      monitor ! GCheckGuard(nextWhen)
    }
 }

  override protected def reset(): Unit = {
    super.reset()
    whenList.clear()
  }

  protected[runtime] def guardResult(when: Node, pass: Boolean): Unit = {
    trace("GUARDRES", s"$node $when: $pass", logEvent)

    if (pass) {
      // Force all the rest of the branches to abort;
      // this avoids dead letters, but I'm not sure it's
      // the best solution.
      for (child <- whenList) {
        stopUnselectedBranch(child)
      }
      // Run the one that passed
      monitor ! GStart(when)
    } else {
      stopUnselectedBranch(when)
      if (whenList.isEmpty) {
        // nop? What do we do if no branch passes?
        trace("NOBRANCH", "Ran off the end of the whenList", logEvent)
      } else {
        val nextWhen = whenList.head
        whenList.remove(0)
        monitor ! GCheckGuard(nextWhen)
      }
    }
  }

  private def stopUnselectedBranch(node: Node): Unit = {
    trace("KILLBRANCH", s"${this.node} $node", logEvent)
    monitor ! GAbort(node)
    for (output <- node.outputs) {
      monitor ! GClose(node, output)
    }
  }

  override protected def traceMessage(code: String, details: String): String = {
    s"$code          ".substring(0, 10) + details + " [Choose]"
  }
}
