package com.jafpl.graph

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import com.jafpl.graph.GraphMonitor._

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
  case class GWaitingFor(node: Node, ports: List[String], dependsOn: List[Node])
  case class GFinished()
  case class GDump()
  case class GTrace(enable: Boolean)
}

class GraphStatus(val ports: List[String], val dependsOn: List[Node]) {
}

class GraphMonitor(private val graph: Graph) extends Actor {
  val log = Logging(context.system, this)
  val watching = mutable.HashSet.empty[Node]
  val _status = mutable.HashMap.empty[Node, GraphStatus]
  val subgraphs = mutable.HashMap.empty[ActorRef, List[Node]]
  var trace = false

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
    log.info("===============================================================Graph Monitor==")
    for (node <- watching) {
      var msg = "WATCHING: " + node
      if (_status.contains(node)) {
        if (_status(node).ports.nonEmpty) {
          msg += " (waiting for:"
          for (port <- _status(node).ports) {
            msg += " " + port
          }
          msg += ")"
        }

        if (_status(node).dependsOn.nonEmpty) {
          msg += " (depends on:"
          for (dep <- _status(node).dependsOn) {
            msg += " " + dep
          }
          msg += ")"
        }
      }

      log.info(msg)
    }
    for (ref <- subgraphs.keySet) {
      log.info("SUBGRAPH: " + ref)
      for (node <- subgraphs(ref)) {
        log.info("            " + node)
      }
    }
    log.info("===============================================================/Graph Monitor=")
  }

  def status(): Unit = {
    dumpState()
  }

  final def receive = {
    case GWatch(node) =>
      if (trace) {
        log.info("M WATCH   {}", node)
      }
      watch(node)
    case GStart(node) =>
      if (trace) {
        log.info("M START   {}", node)
      }
      start(node)
    case GFinish(node) =>
      if (trace) {
        log.info("M FINISH  {}", node)
      }
      finish(node)
    case GSubgraph(ref, subpipline) =>
      if (trace) {
        var str = ""
        for (node <- subpipline) {
          str += node + " "
        }
        log.info("M SUBGRAF {}: {}", ref, str)
      }
      subgraph(ref, subpipline)
    case GWaitingFor(node, ports, depends) =>
      if (trace) {
        var str = ""
        for (port <- ports) {
          str += port + " "
        }
        for (node <- depends) {
          str += node + " "
        }
        log.info("M WAITFOR {}: {}", node, str)
      }
      _status.put(node, new GraphStatus(ports, depends))
    case GTrace(enable) =>
      if (trace) {
        log.info("M TRACE   {}", enable)
      }
      trace = enable
    case GDump() =>
      dumpState()
    case m: Any => log.debug("Unexpected message: {}", m)
  }
}
