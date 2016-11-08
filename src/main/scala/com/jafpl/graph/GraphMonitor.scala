package com.jafpl.graph

import java.time.{Duration, Instant}

import akka.actor.{Actor, ActorRef}
import akka.event.Logging
import com.jafpl.graph.GraphMonitor._
import com.jafpl.graph.NodeActor.{NException, NFinished, NReset, NRun}
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
  case class GStart(node: Node)
  case class GFinish(node: Node)
  case class GSubgraph(node: Node, subpipeline: List[Node])
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
  val NOTREADY, READY, RUNNING, LOOPING, WAITING, NEEDSRESET, WAITTOFINISH, FINISHED = Value
}

class GraphMonitor(private val graph: Graph) extends Actor {
  val log = Logging(context.system, this)
  val nodes = mutable.HashMap.empty[Node, ActorRef]
  val subgraphs = mutable.HashMap.empty[ActorRef, List[Node]]
  val parents = mutable.HashMap.empty[Node, Node]
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
    //dumpState("RUN")
    for (node <- graph.nodes) {
      if (nodeReadyToRun(node)) {
        stepState.put(node, StepState.READY)
      }

      val state = stepState(node)
      if (state == StepState.READY || state == StepState.WAITING || state == StepState.LOOPING) {
        val dInput = dependsOnInputs.get(node)
        val dNodes = dependsOnNodes.get(node)
        val canRun = dInput.isDefined && dInput.get.isEmpty && dNodes.isDefined && dNodes.get.isEmpty
        var run = false

        node match {
          case start: CompoundStart =>
            run = canRun || (state == StepState.LOOPING)

            val end = start.compoundEnd.asInstanceOf[Node]
            run = run && (stepState(end) == StepState.READY)

            if (run) {
              stepState.put(end, StepState.WAITING)
            }
          case end: CompoundEnd =>
            run = canRun
          case _: Any =>
            run = canRun && (stepState(node) == StepState.READY)
        }
        if (run) {
          if (trace) {
            log.info("M RUN     {}", node)
          }
          stepState.put(node, StepState.RUNNING)
          nodes(node) ! NRun()
        }
      }
    }
    //dumpState("/RUN")
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
    if (parents.contains(node)) {
      val pnode = parents(node)
      ready = ready && (stepState(pnode) == StepState.RUNNING)
    }
    ready
  }

  def parent(node: Node): Option[Node] = {
    parents.get(node)
  }

  def subgraph(node: Node, subpipeline: List[Node]): Unit = {
    val ref = nodes(node)
    subgraphs.put(ref, subpipeline)
    for (subnode <- subpipeline) {
      if (parents.get(subnode).isDefined) {
        throw new GraphException("Node already has a parent!?")
      }
      parents.put(subnode, node)
    }
  }

  def start(node: Node): Unit = {
    // nop
  }

  def finish(node: Node): Unit = {
    node match {
      case ce: CompoundEnd => finishCompoundEnd(ce)
      case ic: IterationCache =>
        // Iteration caches never finish on their own
        stepState.put(node, StepState.NEEDSRESET)
      case _ => finishStep(node)
    }
    //dumpState("FINISH")
  }

  def finishCompoundEnd(node: CompoundEnd): Unit = {
    val start = node.compoundStart
    val startActor = nodes(start)
    val subgraph = subgraphs(startActor)

    for (child <- subgraph) {
      if (stepState(child) == StepState.RUNNING) {
        stepState.put(node.asInstanceOf[Node], StepState.WAITTOFINISH)
        return
      }
    }

    if (start.asInstanceOf[CompoundStart].runAgain) {
      resetNode(start)
      start match {
        case loop: LoopStart =>
          for (cache <- loop.caches) {
            if (trace) {
              log.info("  RESET   {}", cache)
            }
            stepState.put(cache, StepState.READY)
          }
        case _ => Unit
      }

      for (child <- subgraph) {
        resetNode(child)
      }

      resetNode(node.asInstanceOf[Node])

      run()
    } else {
      for (child <- subgraph) {
        nodes(child) ! NFinished()
        stepState.put(child, StepState.FINISHED)
      }
      startActor ! NFinished()
      stepState.put(node.asInstanceOf[Node], StepState.FINISHED)

      start match {
        case loop: LoopStart =>
          for (cache <- loop.caches) {
            stepState.put(cache, StepState.FINISHED)
          }
        case _ => Unit
      }

      for (port <- node.asInstanceOf[Node].outputs) {
        graph.monitor ! GClose(node.asInstanceOf[Node], port)
      }
    }
  }

  private def resetNode(node: Node): Unit = {
    if (trace) {
      log.info("  RESET   {}", node)
    }
    nodes(node) ! NReset()
    dependsOnInputs.put(node, mutable.HashSet() ++ node.inputs)
    dependsOnNodes.put(node, mutable.HashSet() ++ node.dependsOn)

    node match {
      case cs: CompoundStart =>
        stepState.put(node, StepState.LOOPING)
      case _ =>
        stepState.put(node, StepState.READY)
    }
  }

  def finishStep(node: Node): Unit = {
    var inloop = false
    var pnode = parent(node)
    while (pnode.isDefined) {
      pnode.get match {
        case ls: LoopStart => inloop || ls.runAgain
        case _ => Unit
      }
      pnode = parent(pnode.get)
    }

    var runAgain = false
    var endNode: Option[Node] = None
    node match {
      case ls: LoopStart =>
        runAgain = ls.runAgain
        if (trace) {
          log.info("  AGAIN   {}: {}", node, runAgain)
        }
        inloop = inloop && runAgain
        endNode = Some(ls.compoundEnd)
      case cs: CompoundStart =>
        endNode = Some(cs.compoundEnd)
      case _ => Unit
    }

    if (inloop || runAgain) {
      if (trace) {
        log.info("  LOOP    {}", node)
      }
      stepState.put(node, StepState.WAITING)
    } else {
      val whoCares = ListBuffer.empty[ActorRef]
      for (ref <- subgraphs.keySet) {
        if (subgraphs(ref).contains(node)) {
          whoCares += ref
        }
      }

      for (ref <- whoCares) {
        var count = 0
        for (node <- subgraphs(ref)) {
          if (stepState(node) != StepState.FINISHED) {
            count += 1
          }
        }

        if (count == 0) {
          if (trace) {
            log.info("> TELL " + ref)
          }
          ref ! NFinished()
        } else {
          if (trace) {
            log.info("> " + count + " " + ref)
          }
        }
      }

      if (endNode.isDefined) {
        nodes(endNode.get) ! NRun()
      }

      stepState.put(node, StepState.FINISHED)
    }

    pnode = parent(node)
    if (pnode.isDefined) {
      val ls = pnode.get.asInstanceOf[CompoundStart]
      var le = ls.compoundEnd
      if (stepState(le) == StepState.WAITTOFINISH) {
        finishCompoundEnd(le.asInstanceOf[CompoundEnd])
      }
    }

    var finished = true
    for (node <- nodes.keySet) {
      finished = finished && (stepState(node) == StepState.FINISHED)
    }
    if (finished) {
      log.debug("Pipeline execution complete")
      //dumpState("Finished")
      graph.finish()
      context.system.terminate()
    }
  }

  def bang(node: Node, srcNode: Node, except: Throwable): Unit = {
    var found = false

    for (actor <- subgraphs.keySet) {
      if (subgraphs(actor).contains(node)) {
        log.info("M CAUGHT  {}: {} (forwards to {})", node, except, actor)
        actor ! NException(node, srcNode, except)
        found = true
      }
    }

    if (!found) {
      log.info("M EXCEPT  {}: {}", node, except)
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

  final def receive = {
    case GSend(node, msg) =>
      lastMessage = Instant.now()
      if (trace) {
        log.info("M SEND    {}: {}", node, msg)
      }
      nodes(node) ! msg
    case GClose(node, port) =>
      lastMessage = Instant.now()
      val edge = graph.getSourceEdge(node, port)

      if (trace) {
        if (edge.isDefined) {
          log.info("M CLOSE   {}: {}", edge.get.destination, edge.get.inputPort)
        } else {
          log.info("M CLOSE   NO EDGE FOR {}: {}", node, port)
        }
      }

      if (edge.isDefined) {
        dependsOnInputs(edge.get.destination).remove(edge.get.inputPort)
      }
      run()
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
        log.info("M EXCEPT  {}: {}", node, except)
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
    case GRun() =>
      lastMessage = Instant.now()
      if (trace) {
        log.info("M RUN GRAPH")
      }
      runIfReady()
    case m: Any => log.info("Unexpected message: {}", m)
  }

}
