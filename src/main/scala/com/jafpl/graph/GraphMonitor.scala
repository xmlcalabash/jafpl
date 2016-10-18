package com.jafpl.graph

import java.time.{Duration, Instant}

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
  case class GException(node: Node, srcNode: Node, throwable: Throwable)
  case class GWatchdog(millis: Int)
}

class GraphStatus(val ports: List[String], val dependsOn: List[Node]) {
}

class GraphMonitor(private val graph: Graph) extends Actor {
  val log = Logging(context.system, this)
  val watching = mutable.HashSet.empty[Node]
  val _status = mutable.HashMap.empty[Node, GraphStatus]
  val subgraphs = mutable.HashMap.empty[ActorRef, List[Node]]
  var trace = false
  var lastMessage = Instant.now()

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

  def bang(node: Node, srcNode: Node, except: Throwable): Unit = {
    var found = false

    for (actor <- subgraphs.keySet) {
      if (subgraphs(actor).contains(node)) {
        log.debug("M CAUGHT  {}: {} (forwards to {})", node, except, actor)
        actor ! GException(node, srcNode, except)
        found = true
      }
    }

    if (!found) {
      log.debug("M EXCEPT  {}: {}", node, except)
      graph.abort(Some(srcNode), except)
      context.system.terminate()
    }
  }

  def watchdog(millis: Int): Unit = {
    log.debug("M WTCHDOG {}", millis)
    graph.abort(None, new RuntimeException("Pipeline watchdog timer exceeded " + millis + "ms"))
    context.system.terminate()
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
      lastMessage = Instant.now()
      if (trace) {
        log.info("M WATCH   {}", node)
      }
      watch(node)
    case GStart(node) =>
      lastMessage = Instant.now()
      if (trace) {
        log.info("M START   {}", node)
      }
      start(node)
    case GFinish(node) =>
      lastMessage = Instant.now()
      if (trace) {
        log.info("M FINISH  {}", node)
      }
      finish(node)
    case GSubgraph(ref, subpipline) =>
      lastMessage = Instant.now()
      if (trace) {
        var str = ""
        for (node <- subpipline) {
          str += node + " "
        }
        log.info("M SUBGRAF {}: {}", ref, str)
      }
      subgraph(ref, subpipline)
    case GWaitingFor(node, ports, depends) =>
      lastMessage = Instant.now()
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
    case GException(node, srcNode, except) =>
      lastMessage = Instant.now()
      bang(node, srcNode, except)
    case GTrace(enable) =>
      lastMessage = Instant.now()
      if (trace) {
        log.info("M TRACE   {}", enable)
      }
      trace = enable
    case GDump() =>
      lastMessage = Instant.now()
      dumpState()
    case GWatchdog(millis) =>
      val ns = Duration.between(lastMessage, Instant.now()).toMillis
      if (ns > millis) {
        watchdog(millis)
      }
    case m: Any => log.debug("Unexpected message: {}", m)
  }
}
