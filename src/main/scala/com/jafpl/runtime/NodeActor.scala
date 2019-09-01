package com.jafpl.runtime

import akka.actor.ActorRef
import akka.event.LoggingReceive
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{Node, NodeState, WhenStart}
import com.jafpl.messages.{BindingMessage, Message}
import com.jafpl.runtime.NodeActor.{NAbort, NAborted, NChkReady, NClose, NException, NFinished, NGuardCheck, NGuardReport, NInitialize, NInitialized, NInput, NReady, NReset, NResetted, NRun, NStart, NStarted, NStop, NStopped}
import com.jafpl.runtime.TraceEvent.TraceEvent
import com.jafpl.steps.Manifold

import scala.collection.mutable

private[runtime] object NodeActor {
  case class NWatchdog(millis: Long)
  case class NWatchdogTimeout()
  case class NInitialize(parent: Option[ActorRef], actors: Map[Node,ActorRef], outputs: Map[String,(String,ActorRef)])
  case class NInitialized(node: Node)
  case class NStart()
  case class NStarted(node: Node)
  case class NRun()
  case class NChkReady()
  case class NReady(node: Node)
  case class NReset()
  case class NResetted(node: Node)
  case class NFinished(node: Node)
  case class NStop()
  case class NStopped(node: Node)
  case class NAbort()
  case class NAborted(node: Node)
  case class NInput(fromNode: Node, fromPort: String, toPort: String, message: Message)
  case class NClose(fromNode: Node, fromPort: String, port: String)
  case class NGuardCheck()
  case class NGuardReport(node: Node, pass: Boolean)
  case class NNode(node: Node, actor: ActorRef)
  case class NException(node: Node, cause: Exception)
  case class NAbortExecution()
}

private[runtime] class NodeActor(private val monitor: ActorRef,
                                 override protected val runtime: GraphRuntime,
                                 protected val node: Node) extends TracingActor(runtime) {
  protected var parent: ActorRef = _
  protected var actors: Map[Node,ActorRef] = Map()
  protected var outputs: Map[String,(String,ActorRef)] = Map()
  protected var logEvent: TraceEvent = TraceEvent.NODE
  protected val openInputs = mutable.HashSet.empty[String]
  protected val openOutputs = mutable.HashSet.empty[String]
  protected var inputBuffer = new IOBuffer()
  protected var outputBuffer = new IOBuffer()
  protected var receivedBindings =  mutable.HashMap.empty[String, Message]
  protected var aborted = false

  if (node.step.isDefined) {
    node.step.get.setConsumer(outputBuffer)
  }

  protected def initialize(): Unit = {
    // sender() *not* parent
    configurePorts()
    sender() ! NInitialized(node)
  }

  protected def configurePorts(): Unit = {
    openInputs.clear()
    openOutputs.clear()

    for (input <- node.inputs) {
      openInputs.add(input)
    }

    for (output <- node.outputs) {
      openOutputs.add(output)
      if (openInputs.contains(output)) {
        openInputs -= output
      }
    }

    node.inputCardinalities.clear()
    node.outputCardinalities.clear()
    trace("CONFPORT", s"$node: $openInputs", TraceEvent.NMESSAGES)
  }

  private def bufferInput(port: String, message: Message): Unit = {
    try {
      if (port != "#bindings" && openInputs.contains("#bindings")) {
        inputBuffer.consume(port, message)
        return
      }

      // What if we have been buffering and the #bindings port has just been closed?
      if (inputBuffer.ports.nonEmpty) {
        for (port <- inputBuffer.ports) {
          for (message <- inputBuffer.messages(port)) {
            trace("DELBUF", "$node $port", TraceEvent.NMESSAGES)
            input(port, message)
          }
        }
        inputBuffer.reset()
      }

      port match {
        case "#bindings" =>
          message match {
            case binding: BindingMessage =>
              receivedBindings(binding.name) = binding.message
              trace("DELBIND", s"$node $port", TraceEvent.NMESSAGES)
              input(port, message)
            case _ =>
              trace("DEL!BIND", s"$node $port", TraceEvent.NMESSAGES)
              input(port, message)
          }
        case _ =>
          trace("DELOTHER", s"$node $port", TraceEvent.NMESSAGES)
          input(port, message)
      }
    } catch {
      case ex: Exception =>
        parent ! NException(node, ex)
    }
  }

  private def deliverBufferedInputs(port: String): Unit = {
    try {
      if (inputBuffer.ports.contains(port)) {
        for (message <- inputBuffer.messages(port)) {
          input(port, message)
        }
        inputBuffer.reset(port)
      }
    } catch {
      case ex: Exception =>
        parent ! NException(node, ex)
    }
  }

  private def incrementInputCardinality(node: Node, port: String): Unit = {
    if (port.startsWith("#")) {
      return
    }

    val count = node.inputCardinalities.getOrElse(port, 0L) + 1
    node.inputCardinalities.put(port, count)
    checkInputCardinality(node, port, TraceEvent.INCRCARDINALITY)
  }

  private def incrementOutputCardinality(node: Node, port: String): Unit = {
    if (port.startsWith("#")) {
      return
    }

    val count = node.outputCardinalities.getOrElse(port, 0L) + 1
    node.outputCardinalities.put(port, count)
    checkOutputCardinality(node, port, TraceEvent.INCRCARDINALITY)
  }

  protected def checkInputCardinality(node: Node, port: String, event: TraceEvent): Unit = {
    if (port.startsWith("#")) {
      return
    }

    if (node.state == NodeState.ABORTING || node.state == NodeState.ABORTED) {
      // If the step never actually ran, or crashed, we don't care about its input cardinalities
      return
    }

    val count = node.inputCardinalities.getOrElse(port, 0L)
    trace("CARD↓", s"$node $port $count", event)

    try {
      val ospec = node.manifold.getOrElse(Manifold.ALLOW_ANY)
      if (ospec.inputSpec.cardinality(port).isDefined) {
        ospec.inputSpec.checkInputCardinality(port, count)
      }
    } catch {
      case jafpl: JafplException =>
        aborted = true
        if (jafpl.code == JafplException.INPUT_CARDINALITY_ERROR) {
          trace("CARD!", s"$node $port $count", TraceEvent.CARDINALITY)
        }
        actors(node) ! NException(node, jafpl)
      case ex: Exception =>
        aborted = true
        actors(node) ! NException(node, ex)
    }
  }

  protected def checkOutputCardinality(node: Node, port: String, event: TraceEvent): Unit = {
    if (port.startsWith("#")) {
      return
    }

    if (node.state == NodeState.ABORTING || node.state == NodeState.ABORTED) {
      // If the step never actually ran, or crashed, we don't care about its input cardinalities
      return
    }

    val count = node.outputCardinalities.getOrElse(port, 0L)
    trace("CARD→", s"$node $port $count", event)

    try {
      val ospec = node.manifold.getOrElse(Manifold.ALLOW_ANY)
      if (ospec.outputSpec.cardinality(port).isDefined) {
        ospec.outputSpec.checkOutputCardinality(port, count)
      }
    } catch {
      case jafpl: JafplException =>
        aborted = true
        if (jafpl.code == JafplException.OUTPUT_CARDINALITY_ERROR) {
          trace("CARD!", s"$node $port $count", TraceEvent.CARDINALITY)
        }
        actors(node) ! NException(node, jafpl)
      case ex: Exception =>
        aborted = true
        actors(node) ! NException(node, ex)
    }
  }

  protected def input(port: String, message: Message): Unit = {
    if (aborted) {
      return
    }

    if (outputs.contains(port)) {
      val output = outputs(port)
      val inputport = output._1
      val receiver = output._2
      receiver ! NInput(node, port, inputport, message)
    } else {
      trace("DROPPED", s"$node.$port", TraceEvent.NMESSAGES)
    }
  }

  protected def close(port: String): Unit = {
    // Even if we abort, we have to close the outputs
    if (openInputs.contains(port)) {
      trace("CLOSEI", s"$node: $port", TraceEvent.NMESSAGES)
      openInputs -= port
    } else {
      trace("CLOSE?", s"$node: $port", TraceEvent.NMESSAGES)
    }
    checkIfReady()
  }

  protected def sendMessage(outputport: String, item: Message): Unit = {
    if (aborted) {
      return
    }
    if (outputs.contains(outputport)) {
      val t = outputs(outputport)
      val inputport = t._1
      val receiver = t._2
      receiver ! NInput(node, outputport, inputport, item)
    } else {
      trace("DROPPED", s"$node.$outputport", TraceEvent.NMESSAGES)
    }
  }

  protected def sendClose(outputport: String): Unit = {
    // Even if we abort, we have to close the outputs
    val t = outputs(outputport)
    val inputport = t._1
    val receiver = t._2
    receiver ! NClose(node, outputport, inputport)
  }

  protected def closeOutputs(): Unit = {
    // Even if we abort, we have to close the outputs
    for (port <- openOutputs) {
      sendClose(port)
      openOutputs -= port
    }
  }

  protected def readyToRun: Boolean = {
    !aborted && openInputs.isEmpty && (node.state == NodeState.STARTED || node.state == NodeState.RESET)
  }

  protected def checkIfReady(): Unit = {
    if (readyToRun) {
      trace("READY", s"$node $openInputs ${node.state}", TraceEvent.NMESSAGES)
      parent ! NReady(node)
    } else {
      trace("¬READY", s"$node $openInputs ${node.state}", TraceEvent.NMESSAGES)
    }
  }

  protected def start(): Unit = {
    parent ! NStarted(node)
  }

  protected def started(node: Node): Unit = {
    stateChange(node, NodeState.STARTED)
  }

  protected def ready(node: Node): Unit = {
    stateChange(node, NodeState.READY)
  }

  protected def reset(): Unit = {
    aborted = false
    inputBuffer.reset()
    outputBuffer.reset()
    receivedBindings.clear()
    configurePorts()
    parent ! NResetted(node)
  }

  protected def resetted(node: Node): Unit = {
    // This should never happen, nodes should report that they've been
    // reset only to containers
    throw new RuntimeException("resetted called on NodeActor")
  }

  protected def run(): Unit = {
    if (!aborted) {
      parent ! NFinished(node)
    }
  }

  protected def stop(): Unit = {
    parent ! NStopped(node)
  }

  protected def stopped(node: Node): Unit = {
    stateChange(node, NodeState.STOPPED)
  }

  protected def abort(): Unit = {
    aborted = true
    trace("XXX", s"$node close outputs for abort", TraceEvent.NMESSAGES)
    closeOutputs()
    parent ! NAborted(node)
  }

  protected def aborted(node: Node): Unit = {
    stateChange(node, NodeState.ABORTED)
  }

  protected def finished(node: Node): Unit = {
    stateChange(node, NodeState.FINISHED)
  }

  protected def exceptionHandler(child: Node, ex: Exception): Unit = {
    parent ! NException(node, ex)
  }

  private def protectedArity0(fx: () => Unit): Unit = {
    try {
      fx()
    } catch {
      case ex: Exception =>
        parent ! NException(node, ex)
    }
  }

  private def protectedArity1(fx: (String) => Unit, arg: String): Unit = {
    try {
      fx(arg)
    } catch {
      case ex: Exception =>
        parent ! NException(node, ex)
    }
  }

  private def protectedGuardCheck(): Unit = {
    this match {
      case when: WhenActor =>
        when.guardCheck()
      case _ =>
        throw new RuntimeException(s"Attempt to check guard on $this")
    }
  }

  private def protectedClose(port: String): Unit = {
    close(port)
  }

  private def protectedRun(): Unit = {
    protectedArity0(() => run())
  }

  private def protectedExceptionHandler(child: Node, ex: Exception): Unit = {
    trace("EXCEPTION", s"$child: ${ex.getMessage}", TraceEvent.NMESSAGES)
    try {
      stateChange(child, NodeState.ABORTED)
      exceptionHandler(child, ex)
    } catch {
      case ex: Exception =>
        parent ! NException(node, ex)
    }
  }

  final def receive: PartialFunction[Any, Unit] = {
    LoggingReceive {
      case NInput(fromNode, fromPort, port, message) =>
        runtime.noteMessageTime()
        trace("INPUT", s"$fromNode.$fromPort -> $node.$port", TraceEvent.NMESSAGES)
        incrementOutputCardinality(fromNode, fromPort)
        if (openInputs.contains(port) || (fromNode.parent.isDefined && fromNode.parent.get == node)) {
          incrementInputCardinality(node, port)
        } else {
          incrementOutputCardinality(node, port)
        }
        bufferInput(port, message)
      case NClose(fromNode, fromPort, port) =>
        runtime.noteMessageTime()
        deliverBufferedInputs(port)
        trace("CLOSE", s"$fromNode.$fromPort -> $node.$port", TraceEvent.NMESSAGES)
        checkOutputCardinality(fromNode, fromPort, TraceEvent.CARDINALITY)
        if (openInputs.contains(port)) {
          checkInputCardinality(node, port, TraceEvent.CARDINALITY)
        }
        protectedArity1(protectedClose, port)
      case NStart() =>
        runtime.noteMessageTime()
        trace("START", s"$node", TraceEvent.NMESSAGES)
        start()
      case NStarted(node) =>
        runtime.noteMessageTime()
        trace("STARTED", s"$node", TraceEvent.NMESSAGES)
        started(node)
      case NReady(node) =>
        runtime.noteMessageTime()
        trace("READY", s"$node ${node.state}", TraceEvent.NMESSAGES)
        ready(node)
      case NChkReady() =>
        trace("CHKREADY", s"$node: $openInputs: ${nodeState(node)}", TraceEvent.NMESSAGES)
        checkIfReady()
      case NRun() =>
        runtime.noteMessageTime()
        trace("RUN", s"${nodeState(node)}", TraceEvent.NMESSAGES)
        protectedRun()
      case NStop() =>
        runtime.noteMessageTime()
        trace("STOP", s"$node", TraceEvent.NMESSAGES)
        stop()
      case NStopped(node) =>
        runtime.noteMessageTime()
        trace("STOPPED", s"$node", TraceEvent.NMESSAGES)
        stopped(node)
      case NAbort() =>
        runtime.noteMessageTime()
        trace("ABORT", s"$node", TraceEvent.NMESSAGES)
        abort()
      case NAborted(node) =>
        runtime.noteMessageTime()
        trace("ABORTED", s"$node", TraceEvent.NMESSAGES)
        aborted(node)
      case NReset() =>
        runtime.noteMessageTime()
        trace("RESET", s"$node", TraceEvent.NMESSAGES)
        reset()
      case NResetted(node) =>
        runtime.noteMessageTime()
        trace("RESETTED", s"$node", TraceEvent.NMESSAGES)
        resetted(node)
      case NFinished(node) =>
        runtime.noteMessageTime()
        trace("FINISHED", s"$node", TraceEvent.NMESSAGES)
        finished(node)
      case NGuardCheck() =>
        runtime.noteMessageTime()
        trace("GUARDCHK", s"$node", TraceEvent.NMESSAGES)
        protectedArity0(() => NodeActor.this.protectedGuardCheck())
      case NGuardReport(node, pass) =>
        runtime.noteMessageTime()
        trace("GUARDRPT", s"$node: $pass", TraceEvent.NMESSAGES)
        this match {
          case choose: ChooseActor =>
            choose.guardReport(node.asInstanceOf[WhenStart], pass)
          case _ =>
            throw new RuntimeException(s"Attempt to check guard on $this")
        }
      case NInitialize(parent, actors, outputs) =>
        runtime.noteMessageTime()
        trace("INIT", s"$node", TraceEvent.NMESSAGES)
        if (parent.isDefined) {
          this.parent = parent.get
        } else {
          this.parent = sender()
        }
        this.actors = actors
        this.outputs = outputs
        initialize()
      case NException(child, ex) =>
        runtime.noteMessageTime()
        protectedExceptionHandler(child, ex)
      case m: Any =>
        runtime.noteMessageTime()
        trace("ERROR", s"$m", TraceEvent.NMESSAGES)
        log.error(s"UNEXPECT $m")
    }
  }
}