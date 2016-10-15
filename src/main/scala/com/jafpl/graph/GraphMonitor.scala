package com.jafpl.graph

import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.Receive
import akka.event.Logging
import com.jafpl.graph.GraphMonitor._
import com.jafpl.messages._
import com.jafpl.util.UniqueId

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/8/16.
  */

object GraphMonitor {
  case class GWatch(node: Node)
  case class GStart(node: Node)
  case class GFinish(node: Node)
  case class GSubgraph(ref: ActorRef, subpipeline: List[Node])
  case class GFinished()
}

class GraphMonitor(private val graph: Graph) extends Actor {
  val log = Logging(context.system, this)
  val watching = mutable.HashSet.empty[Node]
  val subgraphs = mutable.HashMap.empty[ActorRef, List[Node]]

  def watch(node: Node): Unit = {
    watching += node
  }

  def subgraph(ref: ActorRef, subpipeline: List[Node]): Unit = {
    var str = ""
    for (node <- subpipeline) { str = str + node + " " }
    subgraphs.put(ref, subpipeline)
  }

  def start(node: Node): Unit = {
    watching += node
  }

  def finish(node: Node): Unit = {
    val whoCares = ListBuffer.empty[ActorRef]
    for (ref <- subgraphs.keySet) {
      if (subgraphs(ref).contains(node)) {
        whoCares += ref
      }
    }

    watching -= node

    for (ref <- whoCares) {
      var count = 0
      for (node <- subgraphs(ref)) {
        if (watching.contains(node)) {
          count += 1
        }
      }

      if (count == 0) {
        log.debug("> TELL " + ref)
        ref ! GFinished()
      } else {
        log.debug("> " + count + " " + ref)
      }
    }

    // dumpState()

    if (watching.isEmpty) {
      log.debug("Pipeline execution complete")
      graph.finish()
      context.system.terminate()
    }
  }

  def dumpState(): Unit = {
    log.info("====================================================================")
    for (node <- watching) {
      log.info("WATCHING: " + node)
    }
    for (ref <- subgraphs.keySet) {
      log.info("SUBGRAPH: " + ref)
      for (node <- subgraphs(ref)) {
        log.info("            " + node)
      }
    }
  }

  final def receive = {
    case GWatch(node) =>
      log.debug("M WATCH  {}", node)
      watch(node)
    case GStart(node) =>
      log.debug("M START  {}", node)
      start(node)
    case GFinish(node) =>
      log.debug("M FINISH {}", node)
      finish(node)
    case GSubgraph(ref, subpipline) =>
      var str = ""
      for (node <- subpipline) {
        str += node + " "
      }
      log.debug("M SUBGRF {}: {}", ref, str)
      subgraph(ref, subpipline)
    case m: Any => log.debug("Unexpected message: {}", m)
  }
}
