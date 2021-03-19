package com.jafpl.runtime

import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{Buffer, CatchStart, ChooseStart, ContainerEnd, Node, WhenStart}
import com.jafpl.messages.Message
import com.jafpl.runtime.NodeState.NodeState
import com.jafpl.steps.PortSpecification
import com.jafpl.util.TraceEventManager

import scala.collection.immutable.HashSet
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class NodeStatus(val node: Node, val action: Action, tracer: TraceEventManager) extends Runnable {
  private var __state = NodeState.LIMBO
  private val _openInputs = mutable.HashSet.empty[String]
  private val _openBindings = mutable.HashSet.empty[String]
  private val _buffers = mutable.HashMap.empty[String, ListBuffer[Message]]
  private val _dependsOn = mutable.HashSet.empty[Node]
  private var inputSpecification = Option.empty[PortSpecification]
  private var outputSpecification = Option.empty[PortSpecification]
  private val inputCardinalities = mutable.HashMap.empty[String,Long]
  private val outputCardinalities = mutable.HashMap.empty[String,Long]

  init()

  private def _state: NodeState = __state
  private def _state_=(state: NodeState): Unit = {
    tracer.trace(s"$node -> $state", TraceEventManager.SCHEDULER)
    __state = state
  }

  private def init(): Unit = {
    node match {
      case end: ContainerEnd =>
        val start = end.start.get
        if (start.manifold.isDefined) {
          // Container ends are just passthroughs; whatever the start can write to,
          // is the input. And the output is exactly what's written in.
          inputSpecification = Some(start.manifold.get.outputSpec)
          outputSpecification = inputSpecification
        }
      case _ =>
        if (node.manifold.isDefined) {
          inputSpecification = Some(node.manifold.get.inputSpec)
          outputSpecification = Some(node.manifold.get.outputSpec)
        }
    }

    doReset(NodeState.RUNNABLE, false)
  }

  def reset(state: NodeState): Unit = {
    doReset(state, true)
  }

  private def doReset(newState: NodeState, resetAction: Boolean): Unit = {
    // On containers, the inputs are determined by the kind of container
    _openInputs.clear()
    for (port <- node.inputs) {
      _openInputs.add(port)
    }

    _buffers.clear()

    _dependsOn.clear()
    node match {
      case end: ContainerEnd =>
        val start = end.start.get
        _dependsOn.add(start)
        for (child <- start.children) {
          _dependsOn.add(child)
        }
      case choose: ChooseStart =>
        // The steps that provide input for the conditions must run first
        for (when <- choose.children) {
          if (when.inputs("condition")) {
            _dependsOn.add(when.inputEdge("condition").from)
          }
        }
      case _ =>
        ()
    }
    // FIXME: deal with #depends_from edges!

    if (resetAction) {
      node match {
        case _: WhenStart =>
          _state = NodeState.LIMBO
        case _: CatchStart =>
          _state = NodeState.LIMBO
        case _ =>
          _state = newState
      }
      action.reset(newState)
      inputCardinalities.clear()
      outputCardinalities.clear()
    }
  }

  def start(): Unit = {
    action.start()
  }

  def abort(): Unit = {
    if (_state != NodeState.ABORTED) {
      _state = NodeState.ABORTED
      action.abort()
    }
  }

  def run(): Unit = {
    action.run()
  }

  def stop(): Unit = {
    _state = NodeState.STOPPED
    action.stop()
  }

  def testWhen(): Boolean = {
    action.asInstanceOf[WhenAction].test
  }

  def loopFinished(): Boolean = {
    action.asInstanceOf[LoopAction].finished()
  }

  /* ================================================================================ */

  def runnable(): Unit = {
    _state = NodeState.RUNNABLE
  }

  def running(): Unit = {
    _state = NodeState.RUNNING
  }

  def looping(): Unit = {
    node match {
      case _: ContainerEnd =>
        doReset(NodeState.RUNNABLE, false)
      case _ =>
        _state = if (node.isInstanceOf[Buffer]) {
          // Buffers are weird. They don't need anything in a loop to run, but
          // we don't want them to run before the loop has started.
          NodeState.LIMBO
        } else  {
          NodeState.RUNNABLE
        }
        _openInputs.clear()
        _openBindings.clear()
        _dependsOn.remove(node)
    }

  }

  def finished(): Unit = {
    _state = NodeState.FINISHED
  }

  /* ================================================================================ */

  protected[runtime] def receivedOn(port: String): Option[Throwable] = {
    val count = inputCardinalities.getOrElse(port, 0L) + 1
    inputCardinalities.put(port, count)
    tracer.trace(s"CARD  INCR I:${node}.${port}: ${count}", TraceEventManager.CARDINALITY)
    checkInputCardinality(port)
  }

  protected[runtime] def checkInputCardinality(port: String): Option[Throwable] = {
    if (_state != NodeState.STOPPED && !node.isInstanceOf[ContainerEnd]) {
      val count = inputCardinalities.getOrElse(port, 0L)
      if (inputSpecification.isDefined) {
        val card = inputSpecification.get.cardinality(port)
        if (card.isDefined) {
          if (!card.get.withinBounds(count)) {
            return Some(JafplException.inputCardinalityError(port, count.toString, card.get))
          }
        } else {
          tracer.trace(s"CARD? No cardinality for input $port on $node", TraceEventManager.CARDINALITY)
        }
      } else {
        tracer.trace(s"CARD? No input specification on $node", TraceEventManager.CARDINALITY)
      }
    }

    None
  }

  protected[runtime] def sentFrom(port: String): Option[Throwable] = {
    val count = outputCardinalities.getOrElse(port, 0L) + 1
    outputCardinalities.put(port, count)
    tracer.trace(s"CARD  INCR O:${node}.${port}: ${count}", TraceEventManager.CARDINALITY)
    checkOutputCardinality(port)
  }

  protected[runtime] def checkOutputCardinality(port: String): Option[Throwable] = {
    if (_state != NodeState.STOPPED) {
      val count = outputCardinalities.getOrElse(port, 0L)
      if (outputSpecification.isDefined) {
        val card = outputSpecification.get.cardinality(port)
        if (card.isDefined) {
          if (!card.get.withinBounds(count)) {
            return Some(JafplException.outputCardinalityError(port, count.toString, card.get))
          }
        } else {
          tracer.trace(s"CARD? No cardinality for output $port on $node", TraceEventManager.CARDINALITY)
        }
      } else {
        tracer.trace(s"CARD? No output specification on $node", TraceEventManager.CARDINALITY)
      }
    }

    None
  }

  def receive(port: String, message: Message): Unit = {
    action.receive(port, message)
  }

  def state(): NodeState.Value = {
    if (_state == NodeState.RUNNABLE) {
      if (_openInputs.isEmpty && _openBindings.isEmpty && _dependsOn.isEmpty) {
        _state = NodeState.READY;
      }
    }
    _state
  }

  def openInputs: Set[String] = HashSet.from(_openInputs)
  def openBindings: Set[String] = HashSet.from(_openBindings)
  def dependsOn: Set[Node] = _dependsOn.toSet

  def close(port: String): Unit = {
    _openInputs.remove(port)
  }

  def reportFinished(node: Node): Unit = {
    _dependsOn.remove(node)
  }

  def buffered(port: String): Int = {
    _buffers.synchronized {
      if (_buffers.contains(port)) {
        _buffers(port).length
      } else {
        0
      }
    }
  }

  def buffer(port: String, message: Message): Unit = {
    _buffers.synchronized {
      if (!_buffers.contains(port)) {
        _buffers.put(port, ListBuffer.empty[Message])
      }
      _buffers(port) += message
    }
  }

  def unbuffer(port: String): ListBuffer[Message] = {
    _buffers.synchronized {
      if (_buffers.contains(port)) {
        val lb = _buffers(port)
        _buffers.remove(port)
        lb
      } else {
        ListBuffer.empty[Message]
      }
    }
  }

  override def toString: String = {
    var waiting = ""
    var sep = ""
    if (_openInputs.nonEmpty) {
      waiting += "O:"
      var xsep = ""
      for (port <- _openInputs) {
        waiting += xsep + port
        xsep = ","
      }
      sep = ", "
    }
    if (_openBindings.nonEmpty) {
      waiting += sep + "B:"
      var xsep = ""
      for (bind <- _openBindings) {
        waiting += xsep + bind
        xsep = ","
      }
      sep = ", "
    }
    if (_dependsOn.nonEmpty) {
      waiting += sep + "D:"
      sep = ""
      for (dep <- _dependsOn) {
        waiting += sep + dep
        sep = ","
      }
      waiting += ")"
    }
    if (waiting != "") {
      waiting = " (" + waiting + ")"
    }

    node.toString + ": " + _state + waiting
  }
}
