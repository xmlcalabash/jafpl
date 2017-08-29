package com.jafpl.runtime

import akka.actor.ActorRef
import com.jafpl.graph.{ContainerStart, Joiner, Node, Splitter}
import com.jafpl.runtime.GraphMonitor.{GAbort, GCheckGuard, GClose, GStart}

import scala.collection.mutable

private[runtime] class ChooseActor(private val monitor: ActorRef,
                                 private val runtime: GraphRuntime,
                                 private val node: ContainerStart) extends StartActor(monitor, runtime, node) {
  var chosen = Option.empty[Node]
  val guards = mutable.HashMap.empty[Node, Option[Boolean]]
  val stopped = mutable.HashMap.empty[Node, Option[Boolean]]

  override protected def start(): Unit = {
    readyToRun = true

    for (child <- node.children) {
      child match {
        case join: Joiner =>
          monitor ! GStart(join)
        case split: Splitter =>
          monitor ! GStart(split)
        case _ =>
          guards.put(child, None)
          stopped.put(child, None)
          monitor ! GCheckGuard(child)
      }
    }
  }

  protected[runtime] def guardResult(when: Node, pass: Boolean): Unit = {
    guards.put(when, Some(pass))

    var stillWaiting = false
    if (chosen.isEmpty) {
      // If we haven't picked one yet, loop through and find the first
      // child that returned true. Note that results may be returned
      // out of order, so if we encounter a child for whom we don't yet
      // have results before we encounter a child for whom the result
      // was 'pass', we have to wait until the next guard result
      for (child <- node.children) {
        child match {
          case join: Joiner => Unit
          case split: Splitter => Unit
          case _ =>
            val guard = guards(child)
            stillWaiting = stillWaiting || guard.isEmpty

            if (chosen.isEmpty && !stillWaiting) {
              if (guard.get) {
                chosen = Some(child)
                monitor ! GStart(child)
              }
            }
        }
      }

      if (chosen.isDefined) {
        // If we just picked one, make sure we stop all the other
        // children that we've seen but didn't select
        for (child <- node.children) {
          child match {
            case join: Joiner => Unit
            case split: Splitter => Unit
            case _ =>
              val fin = stopped(child)
              if (fin.isEmpty) {
                stopped.put(child, Some(child != chosen.get))
                if (child != chosen.get) {
                  stopUnselectedBranch(child)
                }
              }
          }
        }
      } else {
        // If we *haven't* picked one and we aren't still waiting for
        // some results, then there was no condition which passed,
        // stop all the branches
        if (!stillWaiting) {
          for (child <- node.children) {
            child match {
              case join: Joiner => Unit
              case split: Splitter => Unit
              case _ =>
                val fin = stopped(child)
                if (fin.isEmpty) {
                  stopped.put(child, Some(true))
                  stopUnselectedBranch(child)
                }
            }
          }
        }
      }
    } else {
      // If we've already chosen, then stop this branch regardless
      stopUnselectedBranch(when)
    }
  }

  private def stopUnselectedBranch(node: Node): Unit = {
    trace(s"KILLC $node", "Choose")
    monitor ! GAbort(node)
    for (output <- node.outputs) {
      monitor ! GClose(node, output)
    }
  }
}
