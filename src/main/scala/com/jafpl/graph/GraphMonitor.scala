package com.jafpl.graph

import java.time.{Duration, Instant}

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import com.jafpl.graph.GraphMonitor._
import com.jafpl.graph.StepState.StepState
import com.jafpl.messages.ItemMessage

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/8/16.
  */

object GraphMonitor {
  case class GInitialize(node: Node, actor: ActorRef)
  case class GRun()
  case class GWatch(node: Node)
  case class GStart(node: Node)
  case class GFinish(node: Node)
  case class GSubgraph(node: Node, subpipeline: List[Node])
  case class GFinished()
  case class GDump()
  case class GTrace(enable: Boolean)
  case class GException(node: Node, srcNode: Node, throwable: Throwable)
  case class GWatchdog(millis: Int)
  case class GSend(node: Node, msg: ItemMessage)
  case class GClose(node: Node, port: String)
  case class GSelectWhen(choose: Node, when: Node)
}

object GraphState extends Enumeration {
  type GraphState = Value
  val NOTREADY, READY, RUNNING, FINISHED = Value
}

object StepState extends Enumeration {
  type StepState = Value
  val NOTREADY, READY, RUNNING, FINISHED = Value
}

class GraphMonitor(private val graph: Graph) extends Actor {
  val log = Logging(context.system, this)
  val nodes = mutable.HashMap.empty[Node, ActorRef]
  val watching = mutable.HashSet.empty[Node]
  val subgraphs = mutable.HashMap.empty[ActorRef, List[Node]]
  val containers = mutable.HashMap.empty[Node, ListBuffer[Node]]
  val stepState = mutable.HashMap.empty[Node, StepState]
  var graphState = GraphState.NOTREADY

  val dependsOnInputs = mutable.HashMap.empty[Node, mutable.HashSet[String]]
  val dependsOnNodes = mutable.HashMap.empty[Node, mutable.HashSet[Node]]

  var trace = true
  var lastMessage = Instant.now()

  for (node <- graph.nodes) {
    stepState.put(node, StepState.NOTREADY)
  }

  def initialize(node: Node, actor: ActorRef): Unit = {
    nodes.put(node, actor)
    dependsOnInputs.put(node, mutable.HashSet() ++ node.inputs)
    dependsOnNodes.put(node, mutable.HashSet() ++ node.dependsOn)

    var ready = true
    for (node <- graph.nodes) {
      node match {
        case end: CompoundEnd => Unit
        case _ =>
          ready = ready && nodes.contains(node)
      }
    }

    if (ready) {
      graphState = GraphState.READY
    }
  }

  def runIfReady(): Unit = {
    graphState synchronized  {
      if (graphState == GraphState.READY) {
        graphState = GraphState.RUNNING
        run()
      }
    }
  }

  def run(): Unit = {
    dumpState("RUN")
    for (node <- graph.nodes) {
      if (nodeReadyToRun(node)) {
        stepState.put(node, StepState.READY)
      }

      if (stepState(node) == StepState.READY) {
        node match {
          case end: CompoundEnd => Unit
          case _: Any =>
            if (dependsOnInputs(node).isEmpty && dependsOnNodes(node).isEmpty) {
              stepState.put(node, StepState.RUNNING)
              nodes(node) ! GRun()
            }
        }
      }
    }
    dumpState("/RUN")
  }

  def nodeReadyToRun(node: Node): Boolean = {
    // Only nodes currently in the NOTREADY state should be considered
    // (i.e., we don't consider RUNNING or FINISHED nodes)
    var ready = stepState(node) == StepState.NOTREADY

    node match {
      case input: InputNode =>
        // nop
      case _ =>
        ready = ready && parentIsReady(node)
    }

    if (ready) {
      // Check for choose/when
      // Check for try/catch
    }

    ready
  }

  def parentIsReady(node: Node): Boolean = {
    var ready = true
    if (containers.contains(node)) {
      for (pnode <- containers(node)) {
        ready = ready && (stepState(pnode) == StepState.RUNNING)
      }
    }
    ready
  }

  def watch(node: Node, actor: ActorRef): Unit = {
    if (nodes.contains(node)) {
      if (nodes(node) != actor) {
        throw new IllegalStateException("Only one actor is allowed for any given node")
      }
    } else {
      nodes.put(node, actor)
    }
    watch(node)
  }

  def watch(node: Node): Unit = {
    watching += node
  }

  def subgraph(node: Node, subpipeline: List[Node]): Unit = {
    val ref = nodes(node)
    subgraphs.put(ref, subpipeline)
    for (subnode <- subpipeline) {
      if (!containers.contains(subnode)) {
        containers.put(subnode, ListBuffer.empty[Node])
      }
      containers(subnode) += node
    }
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
        if (trace) {
          log.info("> TELL " + ref)
        }
        ref ! GFinished()
      } else {
        if (trace) {
          log.info("> " + count + " " + ref)
        }
      }
    }

    stepState.put(node, StepState.FINISHED)

    // dumpState()
    var finished = true
    for (node <- nodes) {
      finished = finished && (stepState(node) == StepState.FINISHED)
    }
    if (finished) {
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
    dumpState("WATCHDOG")
    graph.abort(None, new RuntimeException("Pipeline watchdog timer exceeded " + millis + "ms"))
    context.system.terminate()
  }

  def tell(node: Node, msg: Any): Unit = {
    nodes(node) ! msg
  }

  def dumpState(): Unit = {
    dumpState("")
  }

  def dumpState(msg: String): Unit = {
    log.info("===============================================================Graph Monitor: {}", msg)

    for (node <- graph.nodes) {
      var msg = "NODE: " + node + " " + stepState(node) + ": "
      if (dependsOnInputs.get(node).isDefined) {
        val ports = dependsOnInputs(node)
        if (ports.nonEmpty) {
          msg += " (waiting for:"
          for (port <- ports) {
            msg += " " + port
          }
          msg += ")"
        }
        val depends = dependsOnNodes(node)
        if (depends.nonEmpty) {
          msg += " (depends on:"
          for (dep <- depends) {
            msg += " " + dep
          }
          msg += ")"
        }
      } else {
        msg += " (uninitialized)"
      }

      log.info(msg)
    }

    log.info("===============================================================/Graph Monitor=")
  }

  def status(): Unit = {
    dumpState()
  }

  final def receive = {
    case GSend(node, msg) =>
      lastMessage = Instant.now()
      if (trace) {
        log.info("M SEND    {}: {}", node, msg)
      }
      nodes(node) ! msg
    case GClose(node, port) =>
      lastMessage = Instant.now()
      if (trace) {
        log.info("M CLOSE   {}: {}", node, port)
      }
      dependsOnInputs(node).remove(port)
      run()
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
    case GSubgraph(node, subpipline) =>
      lastMessage = Instant.now()
      if (trace) {
        var str = ""
        for (node <- subpipline) {
          str += node + " "
        }
        log.info("M SUBGRAF {}: {}", node, str)
      }
      subgraph(node, subpipline)
    case GException(node, srcNode, except) =>
      lastMessage = Instant.now()
      if (trace) {
        log.info("M EXCEPT  {}", node)
      }
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
    case GInitialize(node, actor) =>
      lastMessage = Instant.now()
      if (trace) {
        log.info("M INIT    {}", node)
      }
      initialize(node, actor)
      runIfReady()
    case GRun() =>
      lastMessage = Instant.now()
      if (trace) {
        log.info("M RUN GRAPH")
      }
      runIfReady()
    case m: Any => log.info("Unexpected message: {}", m)
  }

  /*
    final def receive = {
    case GSend(node, msg) =>
      if (trace) {
        log.info("M SEND    {}: {}", node, msg)
        nodes(node) ! msg
      }
    case GClose(node, port) =>
      if (trace) {
        log.info("M CLOSE   {}: {}", node, port)
      }
      dependsOnInput(node).remove(port)
      run()
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
    case GSubgraph(node, subpipline) =>
      lastMessage = Instant.now()
      if (trace) {
        var str = ""
        for (node <- subpipline) {
          str += node + " "
        }
        log.info("M SUBGRAF {}: {}", node, str)
      }
      subgraph(node, subpipline)
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
    case GInitialize(node, actor) =>
      lastMessage = Instant.now()
      initialize(node, actor)
      runIfReady()
    case GRun() =>
      runIfReady()
    case m: Any => log.info("Unexpected message: {}", m)
  }

   */
}
