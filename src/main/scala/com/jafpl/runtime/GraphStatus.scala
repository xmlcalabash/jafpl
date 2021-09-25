package com.jafpl.runtime

import com.jafpl.exceptions.JafplException
import com.jafpl.graph.{AtomicNode, Binding, Buffer, CatchStart, ChooseStart, ContainerEnd, ContainerStart, Edge, EmptySource, FinallyStart, GraphInput, GraphOutput, GroupStart, Joiner, LoopEachStart, LoopForStart, LoopStart, LoopUntilStart, LoopWhileStart, Node, PipelineStart, Sink, Splitter, TryCatchStart, TryStart, ViewportStart, WhenStart}
import com.jafpl.messages.Message
import com.jafpl.runtime.NodeState.NodeState
import com.jafpl.util.TraceEventManager

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

class GraphStatus(val scheduler: Scheduler) {
  // In this class, all access to nodeStatus is synchronized, perhaps more aggressively than absolutely necessary
  private val nodeStatus = mutable.HashMap.empty[Node, NodeStatus]
  private val dependentOn = mutable.HashMap.empty[Node, ListBuffer[Node]]
  private val tracer = scheduler.runtime.traceEventManager

  dumpGraph()

  for (node <- scheduler.runtime.graph.nodes) {
    createNodeStatus(node)
    node match {
      case container: ContainerStart =>
        createNodeStatus(container.containerEnd)
      case _ => ()
    }
    if (node.parent.isEmpty) {
      tracer.trace(s"Top-level node is runnable: ${node}", TraceEventManager.GRAPH)
      nodeStatus(node).runnable()
    }
  }

  showState()

  private def dumpGraph(): Unit = {
    tracer.trace("==========================================================================", TraceEventManager.GRAPH)

    val rootNodes = scheduler.runtime.graph.nodes.filter(_.parent.isEmpty)

    for (node <- rootNodes.filter(_.isInstanceOf[GraphInput])) {
      dumpGraph(node, "")
    }

    for (node <- rootNodes) {
      node match {
        case _: GraphInput => ()
        case _: GraphOutput => ()
        case _ => dumpGraph(node, "")
      }
    }

    for (node <- rootNodes.filter(_.isInstanceOf[GraphOutput])) {
      dumpGraph(node, "")
    }

    for (edge <- scheduler.runtime.graph.edges) {
      tracer.trace(s"${edge.from}.${edge.fromPort} -> ${edge.to}.${edge.toPort}", TraceEventManager.GRAPH)
    }

    tracer.trace("==========================================================================", TraceEventManager.GRAPH)
  }

  private def dumpGraph(node: Node, indent: String): Unit = {
    tracer.trace(indent + node, TraceEventManager.GRAPH)
    node match {
      case container: ContainerStart =>
        for (child <- container.children) {
          dumpGraph(child, indent + "  ")
        }
        tracer.trace(indent + container.containerEnd, TraceEventManager.GRAPH)
      case _ => ()
    }
  }

  def state(node: Node): NodeState = {
    tracer.trace("debug", s"MUTEX LOCK for state($node)", TraceEventManager.MUTEX)
    val state = nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for state($node)", TraceEventManager.MUTEX)
      nodeStatus(node).state()
    }
    tracer.trace("debug", s"MUTEX UNLOCK for state($node)", TraceEventManager.MUTEX)
    state
  }

  def openInputs(node: Node): Set[String] = {
    tracer.trace("debug", s"MUTEX LOCK for openInputs($node)", TraceEventManager.MUTEX)
    val inputs = nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for openInputs($node)", TraceEventManager.MUTEX)
      nodeStatus(node).openInputs
    }
    tracer.trace("debug", s"MUTEX UNLOCK for opneInputs($node)", TraceEventManager.MUTEX)
    inputs
  }

  def run(node: Node): Unit = {
    tracer.trace("debug", s"MUTEX LOCK for run($node)", TraceEventManager.MUTEX)
    val status = nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for run($node)", TraceEventManager.MUTEX)
      nodeStatus(node)
    }
    tracer.trace("debug", s"MUTEX UNLOCK for run($node)", TraceEventManager.MUTEX)
    status.run()
  }

  def running(node: Node): Unit = {
    tracer.trace("debug", s"MUTEX LOCK for running($node)", TraceEventManager.MUTEX)
    nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for running($node)", TraceEventManager.MUTEX)
      nodeStatus(node).running()
    }
    tracer.trace("debug", s"MUTEX UNLOCK for running($node)", TraceEventManager.MUTEX)
  }

  def testWhen(node: WhenStart): Boolean = {
    tracer.trace("debug", s"MUTEX LOCK for testWhen($node)", TraceEventManager.MUTEX)
    val test = nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for testWhen($node)", TraceEventManager.MUTEX)
      nodeStatus(node).testWhen()
    }
    tracer.trace("debug", s"MUTEX UNLOCK for testWhen($node): $test", TraceEventManager.MUTEX)
    test
  }

  def finished(node: Node): Unit = {
    tracer.trace("debug", s"MUTEX LOCK for finished($node)", TraceEventManager.MUTEX)
    nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for finished($node)", TraceEventManager.MUTEX)
      nodeStatus(node).finished()
      for (port <- node.outputs) {
        val edge = node.outputEdge(port)
        tracer.trace("SCHED CLOSE " + port + " on " + node, TraceEventManager.SCHEDULER)
        nodeStatus(edge.to).close(edge.toPort)
      }
      if (dependentOn.contains(node)) {
        for (dep <- dependentOn(node)) {
          nodeStatus(dep).reportFinished(node)
        }
      }
    }
    tracer.trace("debug", s"MUTEX UNLOCK for finished($node)", TraceEventManager.MUTEX)
  }

  def checkCardinalities(edge: Edge): Option[Throwable] = {
    var ex = Option.empty[Throwable]

    tracer.trace("debug", s"MUTEX LOCK for cardinalities on $edge", TraceEventManager.MUTEX)
    nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for cardinalities on $edge", TraceEventManager.MUTEX)
      ex = nodeStatus(edge.from).sentFrom(edge.fromPort)
      if (ex.isEmpty && edge.toPort != "#bindings") {
        ex = nodeStatus(edge.to).receivedOn(edge.toPort)
      }
    }
    tracer.trace("debug", s"MUTEX UNLOCK for cardinalities on $edge", TraceEventManager.MUTEX)
    ex
  }

  def checkInputCardinalities(node: Node): Option[Throwable] = {
    var ex = Option.empty[Throwable]
    tracer.trace("debug", s"MUTEX LOCK for checkInputCardinalities for $node", TraceEventManager.MUTEX)
    nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for checkInputCardinalities for $node", TraceEventManager.MUTEX)
      for (port <- node.inputs) {
        if (ex.isEmpty) {
          ex = nodeStatus(node).checkInputCardinality(port)
        }
      }
    }
    tracer.trace("debug", s"MUTEX UNLOCKED for checkInputCardinalities for $node", TraceEventManager.MUTEX)
    ex
  }

  def checkOutputCardinalities(node: Node): Option[Throwable] = {
    var ex = Option.empty[Throwable]
    tracer.trace("debug", s"MUTEX LOCK for checkOutputCardinalities for $node", TraceEventManager.MUTEX)
    nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for checkOutputCardinalities for $node", TraceEventManager.MUTEX)
      for (port <- node.outputs) {
        if (ex.isEmpty) {
          ex = nodeStatus(node).checkOutputCardinality(port)
        }
      }
    }
    tracer.trace("debug", s"MUTEX UNLOCKED for checkOutputCardinalities for $node", TraceEventManager.MUTEX)
    ex
  }

  def receive(fromNode: Node, fromPort: String, toNode: Node, toPort: String, message: Message): Unit = {
    tracer.trace("debug", s"MUTEX LOCK for receive from $fromNode", TraceEventManager.MUTEX)
    nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for receive from $fromNode", TraceEventManager.MUTEX)
      val status = nodeStatus(toNode)
      if (status.state() == NodeState.RUNNING || toNode.isInstanceOf[WhenStart]) {
        tracer.trace(s"SCHED FORW ${fromNode}.${fromPort} → ${toNode}.${toPort}: ${message}", TraceEventManager.MESSAGES)
        status.receive(toPort, message)
      } else {
        status.buffer(toPort, message)
        tracer.trace(s"SCHED BUFR ${fromNode}.${fromPort} → ${toNode}.${toPort}: ${message} (${status.buffered(toPort)})", TraceEventManager.MESSAGES)
      }
    }
    tracer.trace("debug", s"MUTEX UNLOCK for receive from $fromNode", TraceEventManager.MUTEX)
  }

  def unbuffer(node: Node): Unit = {
    tracer.trace("debug", s"MUTEX LOCK for unbuffer($node)", TraceEventManager.MUTEX)
    nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for unbuffer($node)", TraceEventManager.MUTEX)
      val status = nodeStatus(node)
      for (buf <- status.unbuffer()) {
        tracer.trace(s"SCHED UBUF ${buf._2} → ${node}.${buf._1}", TraceEventManager.MESSAGES)
        status.receive(buf._1, buf._2)
      }
    }
    tracer.trace("debug", s"MUTEX UNLOCK for unbuffer($node)", TraceEventManager.MUTEX)
  }

  private def showState(): Unit = {
    if (!tracer.traceEnabled(TraceEventManager.SCHEDULER)) {
      return
    }
    tracer.trace("debug", s"MUTEX LOCK for showState", TraceEventManager.MUTEX)
    nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for showState", TraceEventManager.MUTEX)
      tracer.trace("debug", "----- -------------------------------", TraceEventManager.SCHEDULER)

      val rootNodes = scheduler.runtime.graph.nodes.filter(_.parent.isEmpty)
      for (node <- rootNodes.filter(_.isInstanceOf[GraphInput])) {
        showNodeState(node, "")
      }

      for (node <- rootNodes) {
        node match {
          case _: GraphInput => ()
          case _: GraphOutput => ()
          case _ => showNodeState(node, "")
        }
      }

      for (node <- rootNodes.filter(_.isInstanceOf[GraphOutput])) {
        showNodeState(node, "")
      }

      tracer.trace("debug", "----- -------------------------------", TraceEventManager.SCHEDULER)
    }
    tracer.trace("debug", s"MUTEX UNLOCK for showState", TraceEventManager.MUTEX)
  }

  private def showNodeState(node: Node, indent: String): Unit = {
    tracer.trace("debug", s"RDY?  ${indent}${nodeStatus(node)}", TraceEventManager.SCHEDULER)
    node match {
      case start: ContainerStart =>
        for (child <- start.children) {
          showNodeState(child, s"${indent}  ")
        }
        showNodeState(start.containerEnd, indent)
      case _ => ()
    }
  }

  def findReady(): Option[Node] = {
    showState()
    var found = Option.empty[Node]
    tracer.trace("debug", s"MUTEX LOCK for findReady", TraceEventManager.MUTEX)
    nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for findReady", TraceEventManager.MUTEX)
      // This lazy implementation iterates over the whole graph. In principle,
      // it should be possible to optimize this, but is it ever going to be
      // worth it?
      for (node <- nodeStatus.keys) {
        if (found.isEmpty) {
          val status = nodeStatus(node).state()
          if (status == NodeState.READY) {
            found = Some(node)
          }
        }
      }
    }
    tracer.trace("debug", s"MUTEX UNLOCK for findReady", TraceEventManager.MUTEX)
    found
  }

  def runnable(node: Node): Unit = {
    tracer.trace("debug", s"MUTEX LOCK for runnable($node)", TraceEventManager.MUTEX)
    nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for runnable($node)", TraceEventManager.MUTEX)
      nodeStatus(node).runnable()
    }
    tracer.trace("debug", s"MUTEX UNLOCK for runnable($node)", TraceEventManager.MUTEX)
  }

  def startNode(node: Node): Unit = {
    tracer.trace("debug", s"MUTEX LOCK for startNode($node)", TraceEventManager.MUTEX)
    nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for startNode($node)", TraceEventManager.MUTEX)
      nodeStatus(node).runnable()
      nodeStatus(node).start()
    }
    tracer.trace("debug", s"MUTEX UNLOCK for startNode($node)", TraceEventManager.MUTEX)
  }

  def reset(node: Node, state: NodeState): Unit = {
    tracer.trace("debug", s"MUTEX LOCK for reset($node)", TraceEventManager.MUTEX)
    nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for reset($node)", TraceEventManager.MUTEX)
      nodeStatus(node).reset(state)
    }
    tracer.trace("debug", s"MUTEX UNLOCK for reset($node)", TraceEventManager.MUTEX)
  }

  def loop(node: LoopStart): Unit = {
    tracer.trace("debug", s"MUTEX LOCK for loop($node)", TraceEventManager.MUTEX)
    nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for loop($node)", TraceEventManager.MUTEX)
      nodeStatus(node).looping()
      for (child <- node.children) {
        child match {
          case _: Buffer =>
            nodeStatus(child).looping()
          case _ =>
            nodeStatus(child).reset(NodeState.LIMBO)
        }
      }
      nodeStatus(node.containerEnd).looping()
    }
    tracer.trace("debug", s"MUTEX UNLOCK for loop($node)", TraceEventManager.MUTEX)
  }

  def loopFinished(node: LoopStart): Boolean = {
    tracer.trace("debug", s"MUTEX LOCK for loopFinished($node)", TraceEventManager.MUTEX)
    val finished = nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for loopFinished($node)", TraceEventManager.MUTEX)
      nodeStatus(node).loopFinished()
    }
    tracer.trace("debug", s"MUTEX UNLOCK for loopFinished($node): $finished", TraceEventManager.MUTEX)
    finished
  }

  def abort(): Unit = {
    tracer.trace("debug", s"MUTEX LOCK for abort", TraceEventManager.MUTEX)
    nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for abort", TraceEventManager.MUTEX)
      for (node <- nodeStatus.keys) {
        val status = nodeStatus(node)
        if (status.state() != NodeState.LIMBO && status.state() != NodeState.FINISHED) {
          nodeStatus(node).abort()
        }
      }
    }
    tracer.trace("debug", s"MUTEX UNLOCK for abort", TraceEventManager.MUTEX)
  }

  def abort(node: Node): Unit = {
    tracer.trace("debug", s"MUTEX LOCK for abort($node)", TraceEventManager.MUTEX)
    nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for abort($node)", TraceEventManager.MUTEX)
      abortSynced(node)
    }
    tracer.trace("debug", s"MUTEX UNLOCK for abort($node)", TraceEventManager.MUTEX)
  }

  private def abortSynced(node: Node): Unit = {
    node match {
      case start: ContainerStart =>
        for (child <- start.children) {
          abortSynced(child)
        }
        abortNodeSynced(start.containerEnd)
        abortNodeSynced(start)
      case end: ContainerEnd =>
        abortSynced(end.start.get)
      case _ =>
        abortNodeSynced(node)
    }
  }

  private def abortNodeSynced(node: Node): Unit = {
    nodeStatus(node).abort()
    for (port <- node.outputs) {
      val edge = node.outputEdge(port)
      tracer.trace("SCHED CLOSE " + port + " on " + node, TraceEventManager.SCHEDULER)
      nodeStatus(edge.to).close(edge.toPort)
    }
    if (dependentOn.contains(node)) {
      for (dep <- dependentOn(node)) {
        nodeStatus(dep).reportFinished(node)
      }
    }
  }

  def stop(): Unit = {
    tracer.trace("debug", s"MUTEX LOCK for stop", TraceEventManager.MUTEX)
    nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for stop", TraceEventManager.MUTEX)
      for (node <- nodeStatus.keys) {
        nodeStatus(node).stop()
      }
    }
    tracer.trace("debug", s"MUTEX UNLOCK for stop", TraceEventManager.MUTEX)
  }

  def stop(node: Node): Unit = {
    tracer.trace("debug", s"MUTEX LOCK for stop($node)", TraceEventManager.MUTEX)
    nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED for stop($node)", TraceEventManager.MUTEX)
      nodeStatus(node).stop()
      for (dep <- dependentOn.getOrElse(node, ListBuffer.empty[Node])) {
        nodeStatus(dep).reportFinished(node)
      }
      for (port <- node.outputs) {
        val edge = node.outputEdge(port)
        nodeStatus(edge.to).close(edge.toPort)
      }
    }
    tracer.trace("debug", s"MUTEX UNLOCK for stop($node)", TraceEventManager.MUTEX)
  }

  def finished(): Boolean = {
    tracer.trace("debug", s"MUTEX LOCK to test finished", TraceEventManager.MUTEX)
    var fin = true
    nodeStatus.synchronized {
      tracer.trace("debug", s"MUTEX LOCKED to test finished", TraceEventManager.MUTEX)
      // This lazy implementation iterates over the whole graph. In principle,
      // it should be possible to optimize this, but is it every going to be
      // worth it?
      for (node <- nodeStatus.keys) {
        val status = nodeStatus(node).state()
        if (status == NodeState.READY || status == NodeState.RUNNABLE) {
          fin = false
        }
      }
    }
    tracer.trace("debug", s"MUTEX UNLOCK to test finished", TraceEventManager.MUTEX)
    fin
  }

  private def createNodeStatus(node: Node): Unit = {
    tracer.trace(TraceEventManager.CREATE, node.toString, TraceEventManager.CREATE)
    val action = node match {
      case n: PipelineStart => new PipelineAction(n)
      case n: LoopEachStart => new LoopEachAction(n)
      case n: LoopForStart => new LoopForAction(n)
      case n: LoopUntilStart => new LoopUntilAction(n)
      case n: LoopWhileStart => new LoopWhileAction(n)
      case n: ChooseStart => new ChooseAction(n)
      case n: WhenStart => new WhenAction(n)
      case n: ViewportStart => new ViewportAction(n)
      case n: TryCatchStart => new TryCatchAction(n)
      case n: TryStart => new TryAction(n)
      case n: CatchStart => new CatchAction(n)
      case n: FinallyStart => new FinallyAction(n)
      case n: GroupStart => new GroupAction(n)
      case n: GraphInput => new InputAction(n)
      case n: GraphOutput => new OutputAction(n)
      case n: Binding => new BindingAction(n)
      case n: Splitter => new SplitterAction(n)
      case n: Joiner => new JoinerAction(n)
      case n: Buffer => new BufferAction(n)
      case n: Sink => new SinkAction(n)
      case n: EmptySource => new EmptySourceAction(n)
      case n: AtomicNode => new AtomicAction(n)
      case n: ContainerEnd =>
        if (n.start.isDefined) {
          n.start.get match {
            case s: LoopUntilStart =>
              val end = new LoopUntilEndAction(n)
              end.loopStartAction = nodeStatus(s).action.asInstanceOf[LoopUntilAction]
              end
            case s: LoopWhileStart =>
              val end = new LoopWhileEndAction(n)
              end.loopStartAction = nodeStatus(s).action.asInstanceOf[LoopWhileAction]
              end
            case s: ViewportStart =>
              val end = new ViewportEndAction(n)
              end.loopStartAction = nodeStatus(s).action.asInstanceOf[ViewportAction]
              end
            case _ => new EndAction(n)
          }
        } else {
          new EndAction(n)
        }
      case _ =>
        throw JafplException.unexpecteStepType(node.toString, node.location)
    }
    val status = new NodeStatus(node, action, this, tracer)

    for (dep <- status.dependsOn) {
      if (!dependentOn.contains(dep)) {
        dependentOn.put(dep, ListBuffer.empty[Node])
      }
      dependentOn(dep) += node
    }

    action.initialize(scheduler, node)
    nodeStatus.put(node, status)
  }

}
