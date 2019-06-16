package com.jafpl.graph

import java.io.{File, PrintWriter}

import com.jafpl.config.Jafpl
import com.jafpl.exceptions.{JafplException, JafplLoopDetected}
import com.jafpl.graph.JoinMode.JoinMode
import com.jafpl.messages.Message
import com.jafpl.steps.{Manifold, ManifoldSpecification, PortSpecification, Step, ViewportComposer}
import com.jafpl.util.{ItemComparator, ItemTester, UniqueId}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/** A pipeline graph.
  *
  * This is the fundamental API for constructing pipeline graphs. Once constructed,
  * graphs can be executed with the [[com.jafpl.runtime.GraphRuntime]].
  *
  * Graphs are initially open, meaning that nodes and edges can be added to them,
  * and !valid, meaning that no attempt has been made to validate them.
  *
  * When all of the nodes and edges have been added to a graph, the graph
  * is closed and validated by calling the close() method. Only valid graphs
  * can be executed.
  *
  * Generally speaking, steps are either atomic (in which case you're responsible
  * for providing their implementation) or containers. Each may have an optional
  * label. The labels have no purpose except clarity in error messages and in the
  * graph diagrams. Labels must begin with a letter and may consist of letters,
  * digits, hyphens, and underscores.
  *
  * @constructor A pipeline graph.
  *
  */
class Graph protected[jafpl] (jafpl: Jafpl) {
  protected[jafpl] val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val _nodes = ListBuffer.empty[Node]
  private val _edges = ListBuffer.empty[Edge] // The order of edges in this list is significant
  private var open = true
  private var _valid = false
  private var exception = Option.empty[Throwable]
  private var _dumpGraphTransitions = false
  private var _dumpCount = 0

  protected[jafpl] val statics = mutable.HashSet.empty[Binding]

  protected[graph] def error(cause: Throwable): Unit = {
    if (exception.isEmpty) {
      exception = Some(cause)
    }

    cause match {
      case err: JafplException =>
        jafpl.errorListener.error(err, err.location)
      case _ =>
        jafpl.errorListener.error(cause, None)
    }
  }

  protected[jafpl] def nodes: List[Node] = _nodes.toList

  /** True if the graph is known to be valid. */
  def valid: Boolean = _valid

  def dumpGraphTransitions: Boolean = _dumpGraphTransitions
  def dumpGraphTransitions_=(dump: Boolean): Unit = {
    _dumpGraphTransitions = dump
  }

  /** Adds a pipeline to the graph.
    *
    * @return The constructed Pipeline object.
    */
  def addPipeline(manifold: ManifoldSpecification): PipelineStart = addPipeline(None, manifold)

  /** Adds a pipeline to the graph.
    *
    * @param label A user-defined label.
    * @return The constructed Pipeline object.
    */
  def addPipeline(label: String, manifold: ManifoldSpecification): PipelineStart = addPipeline(Some(label), manifold)

  /** Adds a pipeline to the graph.
    *
    * @param label An optional, user-defined label.
    * @return The constructed Pipeline object.
    */
  def addPipeline(label: Option[String], manifold: ManifoldSpecification): PipelineStart = {
    checkOpen()

    logger.debug("addPipeline {}", label.getOrElse("ANONYMOUS"))

    val end = new ContainerEnd(this)
    val start = new PipelineStart(this, end, manifold, label)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end

    /*
    for (port <- manifold.inputSpec.ports) {
      addInput(start, port)
    }

    for (port <- manifold.outputSpec.ports) {
      addInput(start, port)
    }
    */

    start
  }

  /** Adds a graph input.
    *
    * Graph inputs are values that must be provided at runtime. They're effectively
    * ports into which data can be poured before the pipeline is run.
    *
    * @param node The node that needs the input.
    * @param port The port that needs the input.
    */
  def addInput(node: Node, port: String): Unit = {
    checkOpen()

    logger.debug(s"addInput $node.$port")

    val reqdInput = new GraphInput(this, port, node)
    _nodes += reqdInput
    addEdge(reqdInput, "result", node, port)
  }

  /** Adds a graph output.
    *
    * Graph outputs are places where pipeline outputs can be poured.
    *
    * @param node The node that will produce output.
    * @param port The port port on which it will produce.
    */
  def addOutput(node: Node, port: String): Unit = {
    checkOpen()

    logger.debug(s"addOutput $node.$port")

    val reqdOutput = new GraphOutput(this, port, node)
    _nodes += reqdOutput
    addEdge(node, port, reqdOutput, "source")
  }

  /**
    * FIXME: WRITE THIS
    * @param name The option name
    * @param expression The default initializer for the option
    * @return The binding
    */
  def addOption(name: String, expression: Any): OptionBinding = {
    addOption(name, expression, None, false)
  }

  /**
    * FIXME: WRITE THIS
    * @param name The option name
    * @param expression The default initializer for the option
    * @param options Any implementation-specific options you want to pass
    * @return The binding
    */
  def addOption(name: String, expression: Any, options: Option[Any]): OptionBinding = {
    addOption(name, expression, options, false)
  }

  /**
    * FIXME: WRITE THIS
    * @param name The option name
    * @return The binding
    */
  def addStaticOption(name: String): OptionBinding = {
    addOption(name, None, None, true)
  }

  /**
    * FIXME: WRITE THIS
    * @param name The option name
    * @param options Any implementation-specific options you want to pass
    * @return The binding
    */
  def addStaticOption(name: String, options: Option[Any]): OptionBinding = {
    addOption(name, None, options, true)
  }

  /**
    * FIXME: WRITE THIS
    * @param name The option name
    * @param expression The default initializer for the option
    * @param options Any implementation specific options you want to pass
    * @param static The option is statically computed before the graph runs
    * @return The binding
    */
  private def addOption(name: String, expression: Any, options: Option[Any], static: Boolean): OptionBinding = {
    checkOpen()
    logger.debug("addOption {} {}", name, expression)

    val binding = new OptionBinding(this, name, expression, static, options)
    _nodes += binding

    if (static) {
      statics += binding
    }

    binding
  }

  /** Adds an atomic step to the graph.
    *
    * @param step The step implementation.
    * @param label An optional, user-defined label.
    * @return The constructed atomic.
    */
  protected[graph] def addAtomic(step: Step, label: Option[String]): Node = {
    checkOpen()

    val dlabel = label.getOrElse("")
    logger.debug(s"addAtomic $step, $dlabel")

    val node = new AtomicNode(this, Some(step), label)
    _nodes += node
    node
  }

  /** Adds a group to the graph.
    *
    * @param label An optional, user-defined label.
    * @return The constructed group.
    */
  protected[graph] def addGroup(label: Option[String], manifold: ManifoldSpecification): ContainerStart = {
    checkOpen()

    val dlabel = label.getOrElse("")
    logger.debug(s"addGroup $dlabel")

    val end = new ContainerEnd(this)
    val start = new GroupStart(this, end, manifold, label)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end
    start
  }

  /** Adds a choose to the graph.
    *
    * @param label An optional, user-defined label.
    * @return The constructed choose.
    */
  protected[graph] def addChoose(label: Option[String]): ChooseStart = {
    checkOpen()

    logger.debug("addChoose {}", label.getOrElse("ANONYMOUS"))

    val end = new ContainerEnd(this)
    val start = new ChooseStart(this, end, Manifold.ALLOW_ANY, label)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end
    start
  }

  protected[graph] def addWhen(expression: Any, label: Option[String], manifold: ManifoldSpecification): WhenStart = {
    checkOpen()

    logger.debug("addWhen {} {}", label.getOrElse("ANONYMOUS"), expression)

    val end = new ContainerEnd(this)
    val start = new WhenStart(this, end, label, manifold, expression)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end
    start
  }

  /** Adds a for-each to the graph.
    *
    * @param label An optional, user-defined label.
    * @return The constructed for-each.
    */
  protected[graph] def addForEach(label: Option[String], manifold: ManifoldSpecification): LoopEachStart = {
    checkOpen()

    logger.debug("addForEach {}", label.getOrElse("ANONYMOUS"))

    val end = new ContainerEnd(this)
    val start = new LoopEachStart(this, end, label, manifold)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end
    start
  }

  /** Adds a while to the graph.
    *
    * @param tester The test evaluator.
    * @param label An optional, user-defined label.
    * @return The constructed for-each.
    */
  protected[graph] def addWhile(tester: ItemTester, label: Option[String], manifold: ManifoldSpecification): LoopWhileStart = {
    checkOpen()

    logger.debug("addWhile {}", label.getOrElse("ANONYMOUS"))

    val end = new ContainerEnd(this)
    val start = new LoopWhileStart(this, end, label, manifold, tester)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end
    start
  }

  /** Adds an until to the graph.
    *
    * @param comparator The comparator.
    * @param label An optional, user-defined label.
    * @return The constructed for-each.
    */
  protected[graph] def addUntil(comparator: ItemComparator, label: Option[String], manifold: ManifoldSpecification): LoopUntilStart = {
    checkOpen()

    logger.debug("addUntil {}", label.getOrElse("ANONYMOUS"))

    val end = new ContainerEnd(this)
    val start = new LoopUntilStart(this, end, label, manifold, comparator)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end
    start
  }

  /** Adds a for-loop to the graph.
    *
    * @param label An optional, user-defined label.
    * @return The constructed for-each.
    */
  protected[graph] def addFor(label: Option[String], countFrom: Long, countTo: Long, countBy: Long, manifold: ManifoldSpecification): LoopForStart = {
    checkOpen()

    logger.debug("addFor {}", label.getOrElse("ANONYMOUS"))

    val end = new ContainerEnd(this)
    val start = new LoopForStart(this, end, label, countFrom, countTo, countBy, manifold)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end

    start
  }

  /** Adds a viewport to the graph.
    *
    * @param composer The viewport composer.
    * @param label An optional, user-defined label.
    * @return The constructed viewport.
    */
  protected[graph] def addViewport(composer: ViewportComposer, label: Option[String]): ViewportStart = {
    checkOpen()

    logger.debug("addViewport {}", label.getOrElse("ANONYMOUS"))

    val end = new ContainerEnd(this)
    val start = new ViewportStart(this, end, label, composer)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end
    start
  }

  /** Adds a try/catch to the graph.
    *
    * @param label An optional, user-defined label.
    * @return The constructed try/catch.
    */
  protected[graph] def addTryCatch(label: Option[String]): TryCatchStart = {
    checkOpen()

    logger.debug("addTryCatch {}", label.getOrElse("ANONYMOUS"))

    val end = new ContainerEnd(this)
    val start = new TryCatchStart(this, end, label)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end
    start
  }

  protected[graph] def addTry(label: Option[String]): TryStart = {
    checkOpen()

    logger.debug("addTry {}", label.getOrElse("ANONYMOUS"))

    val end = new ContainerEnd(this)
    val start = new TryStart(this, end, label)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end
    start
  }

  protected[graph] def addCatch(label: Option[String], codes: List[Any]): CatchStart = {
    checkOpen()

    val dlabel = label.getOrElse("")
    logger.debug(s"addCatch $dlabel $codes")

    val end = new ContainerEnd(this)
    val start = new CatchStart(this, end, label, codes)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end
    start
  }

  protected[graph] def addFinally(label: Option[String]): FinallyStart = {
    checkOpen()

    logger.debug("addFinally {}", label.getOrElse("ANONYMOUS"))

    val end = new ContainerEnd(this)
    val start = new FinallyStart(this, end, label)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end
    start
  }

  protected[graph] def addSplitter(): Splitter = {
    checkOpen()

    logger.debug("addSplitter")

    val node = new Splitter(this)
    _nodes += node
    node
  }

  protected[graph] def addJoiner(): Joiner = {
    addJoiner(JoinMode.MIXED)
  }

  protected[graph] def addJoiner(mode: JoinMode): Joiner = {
    checkOpen()

    logger.debug("addJoiner")

    val node = new Joiner(this, mode)
    _nodes += node
    node
  }

  private def addBuffer(loop: ContainerStart, edge: Edge): Unit = {
    checkOpen()

    logger.debug("addBuffer")

    val node = new Buffer(this)
    _nodes += node
    addEdge(edge.from, edge.fromPort, node, "source")
    addEdge(node, "result", edge.to, edge.toPort)
    _edges -= edge

    loop.addChild(node)
  }

  protected[graph] def addSink(): Sink = {
    checkOpen()

    logger.debug("addSink")

    val node = new Sink(this)
    _nodes += node
    node
  }

  protected[graph] def addEmptySource(): EmptySource = {
    checkOpen()

    logger.debug("addEmptySource")

    val node = new EmptySource(this)
    _nodes += node
    node
  }

  protected[graph] def addVariable(name: String, expression: Any, options: Option[Any]): Binding = {
    checkOpen()

    logger.debug(s"addVariable $name, $expression")

    val binding = new Binding(this, name, expression, false, options)
    _nodes += binding
    binding
  }

  protected[graph] def addStaticVariable(name: String, options: Option[Any]): Binding = {
    checkOpen()

    logger.debug(s"addStaticVariable $name")

    val binding = new Binding(this, name, None,true, options)
    _nodes += binding
    binding
  }

  /** Adds an edge between two nodes in the graph.
    *
    * An edge connects a specific output port on one step to a specific input port on another.
    * Outputs can go to multiple inputs. Inputs can come from multiple outputs. (In the latter case,
    * no guarantees about the order of the arrival of documents is made.)
    *
    * The graph does not know what inputs steps expect or what outputs they provide. Any edges are
    * allowed. If the actual edges do not correspond to the edges expected by the steps at runtime,
    * errors or unexpected results may occur.
    *
    * The following conditions are errors:
    * 1. Crossing the graphs. Edges must be between nodes in the same graph.
    * 1. Loops. No step may have an input directly or indirectly connected to one of its outputs.
    * 1. Reading through walls. Steps inside a container may read from steps outside the container,
    * but steps outside a container cannot "see" the steps inside a container directly. Instead,
    * the inner steps must provide outputs through their container.
    *
    * @param from The source node, the one that will be sending output.
    * @param fromName The name of the output port on the source node.
    * @param to The destination node, the one that will be receiving input.
    * @param toName The name of the input port on the destination node.
    */
  def addEdge(from: Node, fromName: String, to: Node, toName: String): Unit = {
    addEdge(from, fromName, to, toName, JoinMode.MIXED)
  }

  def addOrderedEdge(from: Node, fromName: String, to: Node, toName: String): Unit = {
    addEdge(from, fromName, to, toName, JoinMode.ORDERED)
  }

  def addPriorityEdge(from: Node, fromName: String, to: Node, toName: String): Unit = {
    addEdge(from, fromName, to, toName, JoinMode.PRIORITY)
  }

  def addGatedEdge(from: Node, fromName: String, to: Node, toName: String): Unit = {
    addEdge(from, fromName, to, toName, JoinMode.GATED)
  }

  private def addEdge(from: Node, fromName: String, to: Node, toName: String, mode: JoinMode): Unit = {
    checkOpen()

    logger.debug("addEdge {}.{} -> {}.{}", from, fromName, to, toName)

    // If from and two aren't in the same graph...
    if (! (_nodes.contains(from) && _nodes.contains(to))) {
      error(JafplException.differentGraphs(from.toString, to.toString, from.location))
      return
    }

    var edge: Option[Edge] = None
    val ancestor = commonAncestor(from, to)
    if (ancestor.isDefined && ancestor.get == to) {
      // println(s"patch $from/$to to ${to.asInstanceOf[ContainerStart].containerEnd} for $from.$fromName")
      edge = Some(new Edge(this, from, fromName, to.asInstanceOf[ContainerStart].containerEnd, toName, mode))
      _edges += edge.get
    } else {
      edge = Some(new Edge(this, from, fromName, to, toName, mode))
      _edges += edge.get
    }

    if (mode == JoinMode.PRIORITY) {
      if (edgesTo(edge.get.to, edge.get.toPort).length > 1) {
        throw JafplException.dupPriorityEdge(from.toString, to.toString, from.location)
      }
    }
  }

  /** Adds a binding edge from the in-scope binding for a variable.
    *
    * @param varname The name of the variable.
    * @param to The step that should receive the binding.
    */
  def addBindingEdge(varname: String, to: Node): Unit = {
    checkOpen()

    logger.debug(s"addBindingEdge $varname $to")

    // Find the variable
    val binding = findInScopeBinding(varname, to)
    if (binding.isEmpty) {
      error(JafplException.variableNotInScope(varname.toString, to.toString, to.location))
    } else {
      addBindingEdge(binding.get, to)
    }
  }

  private def findInScopeBinding(varname: String, start: Node): Option[Binding] = {
    if (start.parent.isEmpty) {
      // Look for global ones
      for (node <- _nodes) {
        if (node.parent.isEmpty) {
          node match {
            case bind: Binding =>
              if (bind.name == varname) {
                return Some(bind)
              }
            case _ => Unit
          }
        }
      }
      return None
    }

    var binding = Option.empty[Binding]
    for (child <- start.parent.get.children) {
      child match {
        case bind: Binding =>
          if (bind.name == varname) {
            binding = Some(bind)
          }
        case _ =>
          if (child == start) {
            if (binding.isDefined) {
              return binding
            } else {
              return findInScopeBinding(varname, start.parent.get)
            }
          }
      }
    }

    None // This can't actually happen, but the compiler can't tell.
  }

  /** Adds an edge from a variable binding to a step.
    *
    * @param from The variable binding.
    * @param to The step that should receive the binding.
    */
  def addBindingEdge(from: Binding, to: Node): Unit = {
    checkOpen()

    logger.debug(s"addBindingEdge $from -> $to")

    if (_nodes.contains(from) && _nodes.contains(to)) {
      val edge = new BindingEdge(this, from, to)
      _edges += edge
    } else {
      error(JafplException.differentGraphs(from.toString, to.toString, from.location))
    }
  }

  protected[graph] def addDependsEdge(from: Node, to: Node): Unit = {
    checkOpen()

    logger.debug(s"addDependsEdge $from -> $to")

    if (_nodes.contains(from) && _nodes.contains(to)) {
      val depid = UniqueId.nextId
      val fromName = "#depends_from_" + depid
      val toName = "#depends_to_" + depid

      val edge = new Edge(this, from, fromName, to, toName)
      _edges += edge
    } else {
      error(JafplException.differentGraphs(from.toString, to.toString, from.location))
    }
  }

  protected[jafpl] def inboundPorts(node: Node): Set[String] = {
    val ports = mutable.HashSet.empty[String]
    for (edge <- _edges) {
      if (edge.to == node) {
        edge match {
          //case bedge: BindingEdge => Unit
          case _ => ports.add(edge.toPort)
        }
      }
    }
    ports.toSet
  }

  protected[jafpl] def outboundPorts(node: Node): Set[String] = {
    val ports = mutable.HashSet.empty[String]
    for (edge <- _edges) {
      if (edge.from == node) {
        edge match {
          //case bedge: BindingEdge => Unit
          case _ => ports.add(edge.fromPort)
        }
      }
    }
    ports.toSet
  }

  protected[jafpl] def bindings(node: Node): Set[String] = {
    val varnames = mutable.HashSet.empty[String]
    for (edge <- _edges) {
      if (edge.to == node) {
        edge match {
          case bedge: BindingEdge => varnames.add(bedge.from.name)
          case _ => Unit
        }
      }
    }
    varnames.toSet
  }

  protected[graph] def edges: List[Edge] = _edges.toList

  protected[jafpl] def edgesFrom(node: Node): List[Edge] = {
    val outboundEdges = ListBuffer.empty[Edge]
    for (edge <- _edges) {
      if (edge.from == node) {
        outboundEdges += edge
      }
    }

    outboundEdges.toList
  }

  protected[jafpl] def edgesFrom(node: Node, port: String): List[Edge] = {
    val outboundEdges = ListBuffer.empty[Edge]
    for (edge <- _edges) {
      if (edge.from == node && edge.fromPort == port) {
        outboundEdges += edge
      }
    }

    outboundEdges.toList
  }

  protected[jafpl] def hasEdgeFrom(node: Node, port: String): Boolean = {
    for (edge <- _edges) {
      if (edge.from == node && edge.fromPort == port) {
        return true
      }
    }
    false
  }

  protected[jafpl] def edgesTo(node: Node, port: String): List[Edge] = {
    val inboundEdges = ListBuffer.empty[Edge]
    for (edge <- _edges) {
      if (edge.to == node && edge.toPort == port) {
        inboundEdges += edge
      }
    }

    inboundEdges.toList
  }

  /** Close and validate the graph.
    *
    * When a graph is closed, all of it's nodes and edges will be validated.
    *
    * Closing the graph is transformative. New nodes will be added to the graph to ensure that:
    * - Every output port is connected to a single input port. (Splitters will be added.)
    * - Every input port is connected to a single output port. (Joiners will be added.)
    * - If steps inside a loop read from steps outside a loop, a buffer will be added so that
    *   the second and subsequent iterations can (re)read the input.
    *
    * If exceptions have occurred (or occur during the validation of the graph, the first
    * such exception will be thrown. (All of the exceptions will be sent to the graph error listener,
    * if there is one.)
    *
    * * After a graph is closed, no changes can be made to it.
    * * Only valid graphs can be executed.
    */
  // FIXME: introduce an error listener so that multiple errors can be identified
  def close(): Unit = {
    if (!open) {
      return // let's treat this as harmless
    }

    if (exception.isDefined) {
      throw exception.get
    }

    _valid = true

    debugDumpGraph()

    // Insert exception translators into each catch if the catch reads from the
    // error port and a translator has been provided.
    for (csnode <- nodes.filter(_.isInstanceOf[CatchStart]).filter(_.asInstanceOf[CatchStart].translator.isDefined)) {
      val readers = edgesFrom(csnode, "errors")
      if (readers.nonEmpty) {
        val catchStart = csnode.asInstanceOf[CatchStart]
        logger.debug(s"addAtomic exception translator for ${csnode.step}")
        val node = new AtomicNode(this, catchStart.translator, None)
        _nodes += node

        for (edge <- readers) {
          // Replace this with an edge that reads from the translator
          val newEdge = new Edge(this, node, "result", edge.to, edge.toPort)
          _edges += newEdge
        }
        for (edge <- readers) {
          _edges -= edge
        }

        val newEdge = new Edge(this, catchStart, "errors", node, "source")
        _edges += newEdge

        debugDumpGraph()
      }
    }

    // Make sure all the required edges exist
    for (node <- nodes) {
      node match {
        case atomic: AtomicNode =>
          if (atomic.step.isDefined) {
            var map = mutable.HashSet.empty[String] ++ atomic.step.get.inputSpec.ports
            for (port <- node.inputs) {
              if (map.contains(port)) {
                map -= port
              }
            }
            if (map.nonEmpty) {
              val port = map.toList.head
              error(JafplException.requiredInputMissing(port, atomic.toString, node.location))
            }

            // It's always ok to drop outputs on the floor.

            map = mutable.HashSet.empty[String] ++ atomic.step.get.bindingSpec.bindings
            for (varname <- node.bindings) {
              if (map.contains(varname)) {
                map -= varname
              }
            }
            if (map.nonEmpty) {
              val varname = map.toList.head
              error(JafplException.requiredVariableBindingMissing(varname.toString, atomic.toString, node.location))
            }
          }
        case loop: LoopEachStart =>
          for (in <- loop.inputs) {
            if (in != "source") {
              error(JafplException.badLoopInputPort(in, loop.toString, node.location))
            }
          }

        case bind: Binding =>
          if (edgesFrom(bind).isEmpty) {
            val sink = if (bind.parent.isDefined) {
              bind.parent.get.addSink()
            } else {
              // This is a top-level binding that's unread...
              this.addSink()
            }
            addEdge(bind, "result", sink, "result")
          }
        case when: WhenStart =>
          if (edgesTo(when, "condition").isEmpty) {
            val choose = when.parent.get
            val gparent = choose.parent.get
            val empty = gparent.addEmptySource()
            addEdge(empty, "result", when, "condition")
          }
        case _ => Unit
      }
    }

    debugDumpGraph()

    // For every case where an outbound edge has more than one connection,
    // insert a splitter so that it has only one outbound edge.
    for (node <- nodes) {
      for (port <- node.outputs) {
        val edges = edgesFrom(node, port)
        if (edges.length > 1) {
          logger.debug(s"$node.$port read by multiple steps; adding splitter")
          // Work out what container should contain the splitter
          var container = Option.empty[ContainerStart]
          node match {
            case start: ContainerStart =>
              var input = false
              for (iport <- start.inputs) {
                input = input || (iport == port)
              }
              if (input) {
                container = Some(start)
              }
            case _ => Unit
          }
          if (container.isEmpty) {
            if (node.parent.isDefined) {
              container = node.parent
            } else {
              // Stick it in the pipeline
              var pl = edges.head.to
              while (pl.parent.isDefined) {
                pl = pl.parent.get
              }
              container = Some(pl.asInstanceOf[ContainerStart])
            }
          }

          val splitter = container.get.addSplitter()

          node match {
            case bnode: Binding => addBindingEdge(bnode, splitter)
            case _ => addEdge(node, port, splitter, "source")
          }

          var count = 1
          for (edge <- edges) {
            val oport = "result_" + count
            // Special case; the addEdge method trips up in the bindings case.
            val newEdge = new Edge(this, splitter, oport, edge.to, edge.toPort)
            _edges += newEdge
            count += 1
          }

          for (edge <- edges) {
            _edges -= edge
          }

          debugDumpGraph()
        }
      }
    }

    // Put sinks on unread outputs
    for (node <- nodes) {
      node match {
        case start: ContainerStart =>
          for (port <- node.inputs) {
            val skipLoopSource = start.isInstanceOf[LoopStart] && (port == "source")
            val skipWhenCondition = start.isInstanceOf[WhenStart] && (port == "condition")
            val skipWhenBindings = start.isInstanceOf[WhenStart] && (port == "#bindings")
            val edges = edgesFrom(node, port)
            if (edges.isEmpty && !skipLoopSource && !skipWhenCondition && ! skipWhenBindings) {
              logger.debug(s"Input $port on $start unread, adding sink")
              val sink = start.addSink()
              addEdge(node, port, sink, "source")
              debugDumpGraph()
            }
          }
        case end: ContainerEnd =>
          val start = end.start.get
          for (port <- node.inputs) {
            val skipLoopTest = start.isInstanceOf[LoopStart] && (port == "test")
            if (!start.outputs.contains(port) && !skipLoopTest) {
              logger.debug(s"Output $port on $start unread, adding sink")
              val sink = if (start.parent.isDefined) {
                start.parent.get.addSink()
              } else {
                start.addSink()
              }
              addEdge(start, port, sink, "source")
              debugDumpGraph()
            }
          }
        case atomic: AtomicNode =>
          if (atomic.step.isDefined) {
            for (port <- atomic.step.get.outputSpec.ports) {
              val edges = edgesFrom(node, port)
              if (edges.isEmpty) {
                logger.debug(s"Output $port on $atomic unread, adding sink")
                val start = atomic.parent.get
                val sink = start.addSink()
                addEdge(node, port, sink, "source")
                debugDumpGraph()
              }
            }
          }
        case _ =>
      }
    }

    // If container outputs are read, but nothing writes to them,
    // stick in an EmptySource
    for (node <- nodes) {
      node match {
        case start: ContainerStart =>
          val end = start.containerEnd

          for (port <- start.outputs) {
            val skipCatchErrors   = start.isInstanceOf[CatchStart] && (port == "errors")
            val skipFinallyErrors = start.isInstanceOf[FinallyStart] && (port == "errors")
            val skipLoopCurrent   = start.isInstanceOf[LoopStart] && (port == "current")
            val edges = edgesTo(node, port)
            if (edges.isEmpty && !skipCatchErrors && !skipFinallyErrors && !skipLoopCurrent) {
              val iedges = edgesTo(end, port)
              if (iedges.isEmpty) {
                logger.debug(s"Adding empty source to feed output $start.$port")
                val source = start.addEmptySource()
                addEdge(source, "result", end, port)
                debugDumpGraph()
              }
            }
          }

          // If nothing reads from the loop's current port, stick in a sink
          node match {
            case start: LoopStart =>
              val edges = edgesFrom(node, "current")
              if (edges.isEmpty) {
                logger.debug(s"Adding sink to consume $start.current")
                val sink = start.addSink()
                addEdge(node, "current", sink, "source")
                debugDumpGraph()
              }
              /* I'm not convinced that reading from the error port is implemented correclty yet
            case start: CatchStart =>
              val edges = edgesFrom(node, "error")
              if (edges.isEmpty) {
                logger.debug(s"Adding sink to consume $start.error")
                val sink = start.addSink()
                addEdge(node, "current", sink, "source")
              }
            case start: FinallyStart =>
              val edges = edgesFrom(node, "error")
              if (edges.isEmpty) {
                logger.debug(s"Adding sink to consume $start.error")
                val sink = start.addSink()
                addEdge(node, "current", sink, "source")
              }
              */
            case _ => Unit
          }

        case _ => Unit
      }
    }

    // For every case where an inbound edge has more than one connection,
    // insert a joiner so that it has only one inbound edge
    for (node <- nodes) {
      for (port <- node.inputs) {
        val edges = edgesTo(node, port)
        if (edges.length > 1) {
          // Work out the mode
          var mode = JoinMode.MIXED
          for (edge <- edges) {
            if (edge.mode != JoinMode.MIXED) {
              if (mode == JoinMode.MIXED) {
                mode = edge.mode
              }
            }
          }

          val joiner = if (node.parent.isDefined) {
            node.parent.get.addJoiner(mode)
          } else {
            addJoiner(mode)
          }
          addEdge(joiner, "result", node, port)
          var count = 1
          for (edge <- edges) {
            val iport = "source_" + count
            addEdge(edge.from, edge.fromPort, joiner, iport, mode)
            count += 1
          }

          for (edge <- edges) {
            _edges -= edge
          }

          debugDumpGraph()
        }
      }
    }

    // For every case where an edge crosses from outside a loop
    // into a loop, add a buffer
    var added = true
    while (added) {
      added = false
      for (edge <- _edges) {
        val ancestor = commonAncestor(edge.from, edge.to)
        val isBuffer = edge.to match {
          case buf: Buffer => true
          case _ => false
        }
        if (isBuffer || ancestor.isEmpty || (edge.from == edge.to)) {
          // nevermind, no buffers needed here
        } else {
          var walker = edge.to.parent.get
          var loop = Option.empty[ContainerStart]
          while (walker != ancestor.get) {
            walker match {
              case node: LoopStart =>
                loop = Some(node)
              case _ => Unit
            }
            walker = walker.parent.get
          }

          if (loop.isDefined) {
            added = true
            addBuffer(loop.get, edge)
            debugDumpGraph()
          }
        }
      }
    }

    for (node <- nodes) {
      if (!node.inputsOk()) {
        _valid = false
        error(JafplException.invalidInputs(node.toString, node.location))
      }
      if (!node.outputsOk()) {
        _valid = false
        error(JafplException.invalidOutputs(node.toString, node.location))
      }

      // FIXME: this redundantly checks the same paths more than once
      // Note, however, that checking only nodes without parents (as used to be done here)
      // isn't sufficient. Does starting with nodes with no inbound (binding or port) edges work?
      checkLoops(node, ListBuffer.empty[Node])
    }

    for (edge <- _edges) {
      if (edge.from.step.isDefined && edge.to.step.isDefined
          && !edge.fromPort.startsWith("#depends_from")) {
        val fromCard = edge.from.step.get.outputSpec.cardinality(edge.fromPort)
        val toCard = edge.to.step.get.inputSpec.cardinality(edge.toPort)
        if (fromCard.isEmpty) {
          logger.debug(s"Step ${edge.from.step.get} has no output port named ${edge.fromPort}")
        } else if (toCard.isEmpty) {
          logger.debug(s"Step ${edge.to.step.get} has no input port named ${edge.toPort}")
        } else {
          if (fromCard.get.minimum >= toCard.get.minimum && fromCard.get.maximum <= toCard.get.maximum) {
            // this will be fine
          } else {
            if (fromCard.get.minimum < toCard.get.minimum) {
              logger.debug(s"${edge.from}.${edge.fromPort} may produce fewer documents than ${edge.to}.${edge.toPort} requires")
            }
            if (fromCard.get.maximum > toCard.get.maximum) {
              logger.debug(s"${edge.from}.${edge.fromPort} may produce more documents than ${edge.to}.${edge.toPort} allows")
            }
          }
        }
      }

      val ancestor = commonAncestor(edge.from, edge.to)
      if (ancestor.isDefined) {
        val d1 = depth(ancestor.get, edge.from)
        val d2 = depth(ancestor.get, edge.to)
        if (true) {
          if (d1 > d2) {
            _valid = false
            var from = usefulAncestor(edge.from)
            var to = usefulAncestor(edge.to)
            error(JafplException.readInsideContainer(from.toString,to.toString,d1.toString,d2.toString, from.location))
          }
        } else {
          if (d1 > d2) {
            // Check the special case of reading from the end of a container
            edge.from match {
              case end: ContainerEnd =>
                val d1prime = depth(ancestor.get, end.start.get)
                if (d1prime > d2) {
                  _valid = false
                  var from = usefulAncestor(edge.from)
                  var to = usefulAncestor(edge.to)
                  error(JafplException.readInsideContainer(from.toString,to.toString,d1.toString,d2.toString, from.location))
                }
              case _ =>
                _valid = false
                var from = usefulAncestor(edge.from)
                var to = usefulAncestor(edge.to)
                error(JafplException.readInsideContainer(from.toString,to.toString,d1.toString,d2.toString, from.location))
            }
          }
        }
      }
    }

    open = false
    if (exception.isDefined) {
      _valid = false
      throw exception.get
    }
  }

  private def debugDumpGraph(): Unit = {
    if (dumpGraphTransitions) {
      _dumpCount += 1
      val pw = new PrintWriter(new File(s"dump_${_dumpCount}.xml"))
      pw.write(asXML.toString())
      pw.close()
    }
  }

  private def usefulAncestor(start: Node): Node = {
    var done = false
    var node = start
    while (!done) {
      done = true
      node match {
        case s: Splitter =>
          node = node.parent.get
          done = false
        case j: Joiner =>
          node = node.parent.get
          done = false
        case _ => Unit
      }
    }
    node
  }

  private def depth(ancestor: Node, child: Node): Int = {
    var depth = 0
    var node = child
    while (node != ancestor) {
      depth += 1
      node = node.parent.get
    }
    depth
  }

  private def commonAncestor(node1: Node, node2: Node): Option[Node] = {
    if (node1 == node2) {
      return Some(node1)
    }

    var node = node1
    while (node.parent.isDefined) {
      node = node.parent.get
      if (node == node2) {
        return Some(node)
      }
    }

    if (node2.parent.isDefined) {
      commonAncestor(node1, node2.parent.get)
    } else {
      None
    }
  }

  private def checkLoops(node: Node, path: ListBuffer[Node]): Unit = {
    if (!valid) {
      return
    }

    if (path.contains(node)) {
      _valid = false
      val loopException = new JafplLoopDetected(node.location)
      var loop = ""
      var arrow = ""
      var started = false
      for (pnode <- path) {
        started = started || (pnode == node)
        if (started) {
          loopException.addNode(pnode)
          loop = loop + arrow + pnode
          arrow = "→"
        }
      }
      loopException.addNode(node)
      loop = loop + arrow + node
      error(loopException)
    }

    if (valid) {
      val newpath = path.clone()
      newpath += node

      for (port <- node.outputs) {
        val edges = edgesFrom(node, port)
        for (edge <- edges) {
          checkLoops(edge.to, newpath)
        }
      }
    }
  }

  private def checkOpen(): Unit = {
    if (!open) {
      throw JafplException.graphClosed(None)
    }
  }

  /** Dump all the graph objects to stdout.
    */
  def dump(): Unit = {
    for (node <- _nodes) {
      if (node.parent.isDefined) {
        println(s"$node (${node.parent.get}")
      } else {
        println(s"$node")
      }
    }
    for (edge <- _edges) {
      println(edge)
    }
  }

  /** Return an XML representation of the graph.
    *
    * The graph is in the `http://jafpl.com/ns/graph` namespace. A RELAX NG schema is provided for the grammar.
    * There is also a stylesheet that will transform the graph into an SVG diagram.
    *
    * @return A <graph> element containing a representation of the graph.
    */
  def asXML: xml.Elem = {
    val xmlNodes = ListBuffer.empty[xml.Node]
    xmlNodes += xml.Text("\n")
    for (node <- _nodes) {
      //println(node)
      if (node.parent.isEmpty) {
        xmlNodes += xml.Text("  ")
        xmlNodes += node.dump(4)
        xmlNodes += xml.Text("\n")
      }
    }
    /*
    for (edge <- _edges) {
      println(edge)
    }
    */
    <graph xmlns="http://jafpl.com/ns/graph">{ xmlNodes }</graph>
  }
}
