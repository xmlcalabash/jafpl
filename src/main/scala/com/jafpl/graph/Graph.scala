package com.jafpl.graph

import com.jafpl.exceptions.GraphException
import com.jafpl.steps.{Step, ViewportComposer}
import com.jafpl.util.UniqueId
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
class Graph {
  protected[jafpl] val logger: Logger = LoggerFactory.getLogger(this.getClass)
  private val _nodes = ListBuffer.empty[Node]
  private val _edges = ListBuffer.empty[Edge]
  private var open = true
  private var _valid = false
  private var blewUp = false

  protected[jafpl] def nodes: List[Node] = _nodes.toList

  /** True if the graph is known to be valid. */
  def valid: Boolean = _valid

  /** Adds a pipeline to the graph.
    *
    * @return The constructed Pipeline object.
    */
  def addPipeline(): PipelineStart = addPipeline(None)

  /** Adds a pipeline to the graph.
    *
    * @param label A user-defined label.
    * @return The constructed Pipeline object.
    */
  def addPipeline(label: String): PipelineStart = addPipeline(Some(label))

  /** Adds a pipeline to the graph.
    *
    * @param label An optional, user-defined label.
    * @return The constructed Pipeline object.
    */
  def addPipeline(label: Option[String]): PipelineStart = {
    checkOpen()

    val end = new ContainerEnd(this)
    val start = new PipelineStart(this, end, label)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end
    start
  }

  /**Adds an input requirement.
    *
    * @param node The node that needs the input.
    * @param port The port that needs the input.
    */
  def addInputRequirement(node: Node, port: String): Unit = {
    checkOpen()

    val reqdInput = new InputRequirement(this, port)
    _nodes += reqdInput
    addEdge(reqdInput, "result", node, port)
  }

  /**Adds an output requirement.
    *
    * @param node The node that will produce output.
    * @param port The port port on which it will produce.
    */
  def addOutputRequirement(node: Node, port: String): Unit = {
    checkOpen()

    val reqdOutput = new OutputRequirement(this, port)
    _nodes += reqdOutput
    addEdge(node, port, reqdOutput, "source")
  }

  /** Adds an atomic step to the graph.
    *
    * @param step The step implementation.
    * @return The constructed atomic.
    */
  def addAtomic(step: Step): Node = addAtomic(step, None)

  /** Adds an atomic step to the graph.
    *
    * @param step The step implementation.
    * @param label A user-defined label.
    * @return The constructed atomic.
    */
  def addAtomic(step: Step, label: String): Node = addAtomic(step, Some(label))

  /** Adds an atomic step to the graph.
    *
    * @param step The step implementation.
    * @param label An optional, user-defined label.
    * @return The constructed atomic.
    */
  def addAtomic(step: Step, label: Option[String]): Node = {
    checkOpen()

    val node = new AtomicNode(this, Some(step), label)
    _nodes += node
    node
  }

  /** Adds a group to the graph.
    *
    * @return The constructed group.
    */
  def addGroup(): ContainerStart = addGroup(None)

  /** Adds a group to the graph.
    *
    * @param label A user-defined label.
    * @return The constructed group.
    */
  def addGroup(label: String): ContainerStart = addGroup(Some(label))

  /** Adds a group to the graph.
    *
    * @param label An optional, user-defined label.
    * @return The constructed group.
    */
  def addGroup(label: Option[String]): ContainerStart = {
    checkOpen()

    val end = new ContainerEnd(this)
    val start = new GroupStart(this, end, label)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end
    start
  }

  /** Adds a choose to the graph.
    *
    * @return The constructed choose.
    */
  def addChoose(): ChooseStart = addChoose(None)

  /** Adds a choose to the graph.
    *
    * @param label A user-defined label.
    * @return The constructed choose.
    */
  def addChoose(label: String): ChooseStart = addChoose(Some(label))

  /** Adds a choose to the graph.
    *
    * @param label An optional, user-defined label.
    * @return The constructed choose.
    */
  def addChoose(label: Option[String]): ChooseStart = {
    checkOpen()

    val end = new ContainerEnd(this)
    val start = new ChooseStart(this, end, label)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end
    start
  }

  protected[graph] def addWhen(expression: String, label: Option[String]): WhenStart = {
    checkOpen()

    val end = new ContainerEnd(this)
    val start = new WhenStart(this, end, label, expression)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end
    start
  }

  /** Adds a for-each to the graph.
    *
    * @return The constructed for-each.
    */
  def addForEach(): ForEachStart = addForEach(None)

  /** Adds a for-each to the graph.
    *
    * @param label A user-defined label.
    * @return The constructed for-each.
    */
  def addForEach(label: String): ForEachStart = addForEach(Some(label))

  /** Adds a for-each to the graph.
    *
    * @param label An optional, user-defined label.
    * @return The constructed for-each.
    */
  def addForEach(label: Option[String]): ForEachStart = {
    checkOpen()

    val end = new ContainerEnd(this)
    val start = new ForEachStart(this, end, label)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end
    start
  }

  /** Adds a viewport to the graph.
    *
    * @param composer The viewport composer.
    * @return The constructed viewport.
    */
  def addViewport(composer: ViewportComposer): ViewportStart = addViewport(composer, None)

  /** Adds a viewport to the graph.
    *
    * @param composer The viewport composer.
    * @param label A user-defined label.
    * @return The constructed viewport.
    */
  def addViewport(composer: ViewportComposer, label: String): ViewportStart = addViewport(composer, Some(label))

  /** Adds a viewport to the graph.
    *
    * @param composer The viewport composer.
    * @param label An optional, user-defined label.
    * @return The constructed viewport.
    */
  def addViewport(composer: ViewportComposer, label: Option[String]): ViewportStart = {
    checkOpen()

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
    * @return The constructed try/catch.
    */
  def addTryCatch(): TryCatchStart = addTryCatch(None)

  /** Adds a try/catch to the graph.
    *
    * @param label A user-defined label.
    * @return The constructed try/catch.
    */
  def addTryCatch(label: String): TryCatchStart = addTryCatch(Some(label))

  /** Adds a try/catch to the graph.
    *
    * @param label An optional, user-defined label.
    * @return The constructed try/catch.
    */
  def addTryCatch(label: Option[String]): TryCatchStart = {
    checkOpen()

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

    val end = new ContainerEnd(this)
    val start = new TryStart(this, end, label)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end
    start
  }

  protected[graph] def addCatch(label: Option[String], codes: List[String]): CatchStart = {
    checkOpen()

    val end = new ContainerEnd(this)
    val start = new CatchStart(this, end, label, codes)
    end.parent = start
    end.start = start
    _nodes += start
    _nodes += end
    start
  }

  protected[graph] def addSplitter(): Splitter = {
    checkOpen()

    val node = new Splitter(this)
    _nodes += node
    node
  }

  protected[graph] def addJoiner(): Joiner = {
    checkOpen()

    val node = new Joiner(this)
    _nodes += node
    node
  }

  private def addBuffer(loop: ContainerStart, edge: Edge): Unit = {
    checkOpen()

    val node = new Buffer(this)
    _nodes += node
    addEdge(edge.from, edge.fromPort, node, "source")
    addEdge(node, "result", edge.to, edge.toPort)
    _edges -= edge

    loop.addChild(node)
  }

  /** Add a variable binding to the graph.
    *
    * Variable bindings consist of a name and an expression. The name and expression are
    * arbitrary. At runtime the [[com.jafpl.runtime.ExpressionEvaluator]] provided as
    * part of the [[com.jafpl.runtime.GraphRuntime]] must understand how to evaluate
    * the expression.
    *
    * At runtime, the computed values are provided to steps through binding edges.
    *
    * @param name The variable name.
    * @param expression An expression in a grammar that the evaluator will understand.
    * @return The constructed binding.
    */
  def addBinding(name: String, expression: String): Binding = {
    checkOpen()
    val binding = new Binding(this, name, expression)
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
    checkOpen()

    if (!_nodes.contains(from) || !_nodes.contains(to)) {
      blewUp = true
      throw new GraphException("Both nodes must be in this graph to add an edge")
    }

    val edge = new Edge(this, from, fromName, to, toName)
    _edges += edge
  }

  /** Adds an edge from a variable binding to a step.
    *
    * @param from The variable binding.
    * @param to The step that should receive the binding.
    */
  def addBindingEdge(from: Binding, to: Node): Unit = {
    checkOpen()

    if (!_nodes.contains(from) || !_nodes.contains(to)) {
      blewUp = true
      throw new GraphException("Both nodes must be in this graph to add an edge")
    }

    val edge = new BindingEdge(this, from, to)
    _edges += edge
  }

  protected[graph] def addDependsEdge(from: Node, to: Node): Unit = {
    checkOpen()

    if (!_nodes.contains(from) || !_nodes.contains(to)) {
      blewUp = true
      throw new GraphException("Both nodes must be in this graph to add an edge")
    }

    val depid = UniqueId.nextId
    val fromName = "#depends_from_" + depid
    val toName = "#depends_to_" + depid

    val edge = new Edge(this, from, fromName, to, toName)
    _edges += edge
  }

  protected[jafpl] def inboundPorts(node: Node): Set[String] = {
    val ports = mutable.HashSet.empty[String]
    for (edge <- _edges) {
      if (edge.to == node) {
        edge match {
          case bedge: BindingEdge => Unit
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
          case bedge: BindingEdge => Unit
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

  protected[jafpl] def edgesFrom(node: Node, port: String): List[Edge] = {
    val outboundEdges = ListBuffer.empty[Edge]
    for (edge <- _edges) {
      if (edge.from == node && edge.fromPort == port) {
        outboundEdges += edge
      }
    }
    if (outboundEdges.isEmpty) {
      blewUp = true
      throw new GraphException("Node " + node + " has no output port " + port)
    }
    outboundEdges.toList
  }

  protected[jafpl] def edgesTo(node: Node, port: String): List[Edge] = {
    val inboundEdges = ListBuffer.empty[Edge]
    for (edge <- _edges) {
      if (edge.to == node && edge.toPort == port) {
        inboundEdges += edge
      }
    }
    if (inboundEdges.isEmpty) {
      blewUp = true
      throw new GraphException("Node " + node + " has no input port " + port)
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
    * This step will throw [[com.jafpl.exceptions.GraphException]]s if errors are detected.
    *
    * * After a graph is closed, no changes can be made to it.
    * * Only valid graphs can be executed.
    */
  // FIXME: introduce an error listener so that multiple errors can be identified
  def close(): Unit = {
    if (!open) {
      return // let's treat this as harmless
    }
    _valid = true

    // Make sure all the required edges exist
    for (node <- nodes) {
      node match {
        case atomic: AtomicNode =>
          if (atomic.step.isDefined) {
            var map = mutable.HashSet.empty[String] ++ atomic.step.get.inputSpec.ports()
            for (port <- node.inputs) {
              if (map.contains(port)) {
                map -= port
              }
            }
            if (map.nonEmpty) {
              val port = map.toList.head
              throw new GraphException("Required input '" + port + "' missing: " + atomic)
            }

            map = mutable.HashSet.empty[String] ++ atomic.step.get.outputSpec.ports()
            for (port <- node.outputs) {
              if (map.contains(port)) {
                map -= port
              }
            }
            if (map.nonEmpty) {
              val port = map.toList.head
              throw new GraphException("Required output '" + port + "' missing: " + atomic)
            }

            map = mutable.HashSet.empty[String] ++ atomic.step.get.requiredBindings
            for (varname <- node.bindings) {
              if (map.contains(varname)) {
                map -= varname
              }
            }
            if (map.nonEmpty) {
              val varname = map.toList.head
              throw new GraphException("Required variable binding '" + varname + "' missing: " + atomic)
            }
          }
        case _ => Unit
      }
    }

    // For every case where an outbound edge has more than one connection,
    // insert a splitter so that it has only one outbound edge
    for (node <- nodes) {
      for (port <- node.outputs) {
        val edges = edgesFrom(node, port)
        if (edges.length > 1) {
          val splitter = if (node.parent.isDefined) {
            node.parent.get.addSplitter()
          } else {
            addSplitter()
          }
          addEdge(node, port, splitter, "source")
          var count = 1
          for (edge <- edges) {
            val oport = "result_" + count
            addEdge(splitter, oport, edge.to, edge.toPort)
            count += 1
          }

          for (edge <- edges) {
            _edges -= edge
          }
        }
      }
    }

    // For every case where an inbound edge has more than one connection,
    // insert a joiner so that it has only one inbound edge
    for (node <- nodes) {
      for (port <- node.inputs) {
        val edges = edgesTo(node, port)
        if (edges.length > 1) {
          val joiner = if (node.parent.isDefined) {
            node.parent.get.addJoiner()
          } else {
            addJoiner()
          }
          addEdge(joiner, "result", node, port)
          var count = 1
          for (edge <- edges) {
            val iport = "source_" + count
            addEdge(edge.from, edge.fromPort, joiner, iport)
            count += 1
          }

          for (edge <- edges) {
            _edges -= edge
          }
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
              case node: ForEachStart =>
                loop = Some(node)
              case node: ViewportStart =>
                loop = Some(node)
              case _ => Unit
            }
            walker = walker.parent.get
          }

          if (loop.isDefined) {
            added = true
            addBuffer(loop.get, edge)
          }
        }
      }
    }

    for (node <- nodes) {
      if (!node.inputsOk()) {
        _valid = false
        throw new GraphException("Invalid inputs on " + node)
      }
      if (!node.outputsOk()) {
        _valid = false
        throw new GraphException("Invalid outputs on " + node)
      }
      if (node.parent.isEmpty) {
        checkLoops(node, ListBuffer.empty[Node])
      }
    }

    for (edge <- _edges) {
      if (edge.from.step.isDefined && edge.to.step.isDefined) {
        val fromCard = edge.from.step.get.outputSpec.cardinality(edge.fromPort)
        val toCard = edge.to.step.get.inputSpec.cardinality(edge.toPort)
        if (fromCard.isEmpty) {
          logger.warn(s"Step ${edge.from.step.get} has no output port named ${edge.fromPort}")
        } else if (toCard.isEmpty) {
          logger.warn(s"Step ${edge.to.step.get} has no input port named ${edge.toPort}")
        } else {
          if ((fromCard.get == "1") || (toCard.get == "*") || (fromCard.get == toCard.get)) {
            // nop; this is bound to be fine.
          } else {
            fromCard.get match {
              case "*" =>
                toCard.get match {
                  case "+" =>
                    logger.warn(s"${edge.from}.${edge.fromPort} may produce no output but ${edge.to}.${edge.toPort} requires at least one input")
                  case "1" =>
                    logger.warn(s"${edge.from}.${edge.fromPort} may produce a sequence but ${edge.to}.${edge.toPort} requires exactly one input")
                  case "?" =>
                    logger.warn(s"${edge.from}.${edge.fromPort} may produce a sequence but ${edge.to}.${edge.toPort} requires at most one input")
                  case _ =>
                    throw new GraphException(s"Unexpected cardinality on ${edge.to}.${edge.toPort}: ${toCard.get}")
                }
              case "+" =>
                toCard.get match {
                  case "1" =>
                    logger.warn(s"${edge.from}.${edge.fromPort} may produce a sequence but ${edge.to}.${edge.toPort} requires exactly one input")
                  case "?" =>
                    logger.warn(s"${edge.from}.${edge.fromPort} may produce a sequence but ${edge.to}.${edge.toPort} requires at most one input")
                  case _ =>
                    throw new GraphException(s"Unexpected cardinality on ${edge.to}.${edge.toPort}: ${toCard.get}")
                }
              case "?" =>
                toCard.get match {
                  case "+" =>
                    logger.warn(s"${edge.from}.${edge.fromPort} may produce no output but ${edge.to}.${edge.toPort} requires at least one input")
                  case "1" =>
                    logger.warn(s"${edge.from}.${edge.fromPort} may produce no output but ${edge.to}.${edge.toPort} requires exactly one input")
                  case _ =>
                    throw new GraphException(s"Unexpected cardinality on ${edge.to}.${edge.toPort}: ${toCard.get}")
                }
              case _ =>
                throw new GraphException(s"Unexpected cardinality on ${edge.from}.${edge.fromPort}: ${fromCard.get}")
            }
          }
        }
      }

      val ancestor = commonAncestor(edge.from, edge.to)
      if (ancestor.isDefined) {
        val d1 = depth(ancestor.get, edge.from)
        val d2 = depth(ancestor.get, edge.to)
        if (d1 > d2) {
          _valid = false
          var from = usefulAncestor(edge.from)
          var to = usefulAncestor(edge.to)
          throw new GraphException(s"Attempting to read inside a container: $from -> $to")
        }
      }
    }

    open = false
    _valid = _valid && !blewUp
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
      println("ERROR: Loop detected")
      var started = false
      for (pnode <- path) {
        started = started || (pnode == node)
        if (started) {
          println("  " + pnode)
        }
      }
      println("  " + node)
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
      throw new GraphException("Changes cannot be made to a closed graph")
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
      if (node.parent.isEmpty) {
        xmlNodes += xml.Text("  ")
        xmlNodes += node.dump(4)
        xmlNodes += xml.Text("\n")
      }
    }
    <graph xmlns="http://jafpl.com/ns/graph">{ xmlNodes }</graph>
  }
}
