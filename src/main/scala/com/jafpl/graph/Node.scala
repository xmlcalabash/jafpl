package com.jafpl.graph

import com.jafpl.exceptions.JafplException
import com.jafpl.injection.{PortInjectable, StepInjectable}
import com.jafpl.messages.Message
import com.jafpl.steps.{ManifoldSpecification, PortCardinality, Step}
import com.jafpl.util.UniqueId
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.xml.UnprefixedAttribute

/** A node in the pipeline graph.
  *
  * You can't instantiate nodes directly, see the methods on [[com.jafpl.graph.Graph]] and
  * on [[com.jafpl.graph.ContainerStart]] and its subtypes.
  *
  * @constructor A node in the pipeline graph.
  * @param graph The graph into which this node is to be inserted.
  * @param step An optional implementation step.
  * @param userLabel An optional user-defined label.
  */
abstract class Node(val graph: Graph,
                    val step: Option[Step],
                    val userLabel: Option[String]) {
  protected val logger: Logger = LoggerFactory.getLogger(this.getClass)
  protected[jafpl] val inputInjectables: ListBuffer[PortInjectable] = ListBuffer.empty[PortInjectable]
  protected[jafpl] val outputInjectables: ListBuffer[PortInjectable] = ListBuffer.empty[PortInjectable]
  protected[jafpl] val stepInjectables: ListBuffer[StepInjectable] = ListBuffer.empty[StepInjectable]

  protected[jafpl] val _staticBindings = mutable.HashMap.empty[Binding,Message]
  private var _manifold: Option[ManifoldSpecification] = if (step.isDefined) {
    step
  } else {
    None
  }
  private var _start: Option[ContainerStart] = None
  private var _name: String = if (userLabel.isDefined) {
    userLabel.get
  } else {
    var name = super.toString.split('.').last
    if (name.indexOf('@') > 0) {
      name = name.substring(0, name.indexOf('@'))
    }
    name
  }

  /** A unique identifier for this node.
    *
    * Every node has a unique identifier.
    */
  val id: String = UniqueId.nextId.toString

  private var _loc = if (step.isDefined) {
    step.get.location
  } else {
    Option.empty[Location]
  }

  /** The node's location.
    *
    * The node location will be used for reporting (for example in errors).
    */
  def location: Option[Location] = _loc

  protected[jafpl] def location_=(loc: Location): Unit = {
    _loc = Some(loc)
  }

  def manifold: Option[ManifoldSpecification] = _manifold
  protected[jafpl] def manifold_=(man: ManifoldSpecification): Unit = {
    if (_manifold.isDefined) {
      throw new IllegalArgumentException("Cannot reset a manifold")
    }
    _manifold = Some(man)
  }

  /** The node label.
    *
    * Labels are used in output to help identify the node in question. The `id` of the
    * node is always appended to the label.
    *
    */
  def label: String = s"${_name}-$id"

  protected[graph] def internal_name: String = _name
  protected[graph] def internal_name_=(newname: String): Unit = {
    _name = newname
  }

  /** Add a dependency edge.
    *
    * This method asserts that the current node depends on another node. Ordinarily,
    * data flow establishes dependencies automatically. If step A consumes the output of step B,
    * the pipeline will assure that step B runs before step A.
    *
    * In cases where there is no data flow dependency, but it's still necessary to force an
    * order, you can impose one by saying that `A.dependsOn(B)`.
    *
    * @param node The node in the graph which must be run before this one
    */
  def dependsOn(node: Node): Unit = {
    graph.addDependsEdge(node, this)
  }

  /** The names of this step's input ports.
    *
    * @return The input port names.
    */
  def inputs: Set[String] = {
    graph.inboundPorts(this)
    /*
    if (openInputSet.isEmpty) {
      graph.inboundPorts(this)
    } else {
      openInputSet.get
    }
     */
  }

  /** The names of this step's output ports.
    *
    * @return The output port names.
    */
  def outputs: Set[String] = {
    graph.outboundPorts(this)
    /*
    if (openOutputSet.isEmpty) {
      graph.outboundPorts(this)
    } else {
      openOutputSet.get
    }
     */
  }

  /** The names of this step's variable bindings.
    *
    * This method returns the names of the variables for which this step will receive bindings at runtime.
    *
    * @return The variable names.
    */
  def bindings: Set[String] = graph.bindings(this)

  /** Specify the static bindings that are in scope for this node.
    *
    * Static bindings are known at "compile time" and don't appear in the graph.
    * (You aren't required to have statics in your graph.)
    *
    * This method provides the node with the set of static bindings that are in-scope.
    *
    * @param bindings The static bindings that are in-scope for this node.
    */
  def staticBindings_=(bindings: Map[Binding,Message]): Unit = {
    _staticBindings.clear()
    for ((bind,msg) <- bindings) {
      _staticBindings.put(bind,msg)
    }
  }

  def staticBindings: Map[Binding,Message] = {
    _staticBindings.toMap
  }

  protected[jafpl] def inputEdge(port: String): Edge = {
    graph.edgesTo(this, port).head
  }

  protected[jafpl] def outputEdge(port: String): Edge = {
    graph.edgesFrom(this, port).head
  }

  protected[jafpl] def hasOutputEdge(port: String): Boolean = {
    graph.hasEdgeFrom(this, port)
  }

  /** This node's parent.
    *
    * @return This node's parent.
    */
  def parent: Option[ContainerStart] = _start
  private[graph] def parent_=(node: ContainerStart): Unit = {
    if (_start.isEmpty) {
      _start = Some(node)
    } else {
      throw JafplException.startRedefined(this.toString, node.location)
    }
  }

  def addInputInjectable(injectable: PortInjectable): Unit = {
    inputInjectables += injectable
  }
  def addOutputInjectable(injectable: PortInjectable): Unit = {
    outputInjectables += injectable
  }
  def addStepInjectable(injectable: StepInjectable): Unit = {
    stepInjectables += injectable
  }

  /** A string representation of this node. */
  override def toString: String = {
    s"{$label}"
  }

  protected[graph] def dumpOpenChildren(depth: Int): xml.Node = {
    xml.Text("")
  }

  protected[graph] def dumpClosedChildren(depth: Int): xml.Node = {
    xml.Text("")
  }

  protected[graph] def dumpOpen(depth: Int): xml.Elem = {
    val indent = " " * depth
    val nodes = ListBuffer.empty[xml.Node]
    nodes += xml.Text("\n")

    val inlist = ListBuffer.empty[xml.Node]
    for (input <- inputs) {
      for (edge <- graph.edgesTo(this, input)) {
        inlist += xml.Text("\n")
        inlist += xml.Text(indent + "  ")
        inlist += <in-edge source={ edge.from.id } output-port={ edge.fromPort } input-port={ edge.toPort }></in-edge>
      }
    }
    if (bindings.nonEmpty) {
      for (edge <- graph.edgesTo(this, "#bindings")) {
        inlist += xml.Text("\n")
        inlist += xml.Text(indent + "  ")
        inlist += <in-edge source={ edge.from.id } output-port={ edge.fromPort } input-port={ edge.toPort }></in-edge>
      }
    }

    this match {
      case _: CatchStart =>
        inlist += xml.Text("\n")
        inlist += xml.Text(indent + "  ")
        inlist += <in-edge input-port="error"></in-edge>
      case _: LoopStart =>
        inlist += xml.Text("\n")
        inlist += xml.Text(indent + "  ")
        inlist += <in-edge input-port="current"></in-edge>
      case _ => ()
    }

    if (inlist.nonEmpty) {
      inlist += xml.Text("\n" + indent)
      nodes += xml.Text(indent)
      nodes += <inputs>{ inlist }</inputs>
    }

    if (outputs.nonEmpty) {
      if (inputs.nonEmpty) {
        nodes += xml.Text("\n")
      }

      val outlist = ListBuffer.empty[xml.Node]
      for (output <- outputs) {
        for (edge <- graph.edgesFrom(this, output)) {
          outlist += xml.Text("\n")
          outlist += xml.Text(indent + "  ")
          outlist += <out-edge output-port={ edge.fromPort } input-port={ edge.toPort } destination={ edge.to.id }></out-edge>
        }
      }
      outlist += xml.Text("\n" + indent)
      nodes += xml.Text(indent)
      nodes += <outputs>{ outlist }</outputs>
    }

    nodes += xml.Text("\n" + indent)
    nodes += dumpOpenChildren(depth)

    // Hack for closing indent
    if (indent.length >= 2) {
      nodes += xml.Text("\n" + indent.substring(2))
    } else {
      nodes += xml.Text("\n")
    }

    val className = if (step.isDefined) {
      step.get.getClass.getName
    } else {
      this.getClass.getName
    }
    val shortName = className.split("\\.").last

    val nodeName = this match {
      case start: ContainerStart => "container"
      case end: ContainerEnd => "container-end"
      case _ => "node"
    }

    val extraAttr = this match {
      case start: ContainerStart =>
        new UnprefixedAttribute("end", xml.Text(start.containerEnd.id), xml.Null)
      case end: ContainerEnd =>
        new UnprefixedAttribute("start", xml.Text(end.start.get.id), xml.Null)
      case _ => xml.Null
    }

    // Ba-ar-af. My $DEITY this is ugly.
    val attrs = new xml.UnprefixedAttribute("id", xml.Text(id),
      new UnprefixedAttribute("label", xml.Text(label),
        new UnprefixedAttribute("className", xml.Text(className),
          new UnprefixedAttribute("name", xml.Text(shortName), extraAttr))))
    new xml.Elem(null, nodeName, attrs, xml.TopScope, false, nodes:_*)
  }

  protected[graph] def dumpClosed(depth: Int): xml.Elem = {
    val indent = " " * depth
    val nodes = ListBuffer.empty[xml.Node]
    nodes += xml.Text("\n")

    val inlist = ListBuffer.empty[xml.Node]
    for (input <- inputs) {
      for (edge <- graph.edgesTo(this, input)) {
        inlist += xml.Text("\n")
        inlist += xml.Text(indent + "  ")
        inlist += <in-edge source={ edge.from.id } output-port={ edge.fromPort } input-port={ edge.toPort }></in-edge>
      }
    }
    if (bindings.nonEmpty) {
      for (edge <- graph.edgesTo(this, "#bindings")) {
        inlist += xml.Text("\n")
        inlist += xml.Text(indent + "  ")
        inlist += <in-edge source={ edge.from.id } output-port={ edge.fromPort } input-port={ edge.toPort }></in-edge>
      }
    }

    val outlist = ListBuffer.empty[xml.Node]
    for (output <- outputs) {
      for (edge <- graph.edgesFrom(this, output)) {
        val ancestor = graph.commonAncestor(this, edge.to)
        /*
        if (ancestor.isDefined && ancestor.get == this) {
          inlist += xml.Text("\n")
          inlist += xml.Text(indent + "  ")
          inlist += <out-edge output-port={ edge.fromPort } input-port={ edge.toPort } destination={ edge.to.id }></out-edge>
        } else {
          outlist += xml.Text("\n")
          outlist += xml.Text(indent + "  ")
          outlist += <out-edge output-port={ edge.fromPort } input-port={ edge.toPort } destination={ edge.to.id }></out-edge>
        }
         */
        outlist += xml.Text("\n")
        outlist += xml.Text(indent + "  ")
        outlist += <out-edge output-port={ edge.fromPort } input-port={ edge.toPort } destination={ edge.to.id }></out-edge>
      }
    }

    this match {
      case _: LoopWhileStart =>
        outlist += xml.Text("\n")
        outlist += xml.Text(indent + "  ")
        outlist += <out-edge output-port="test"></out-edge>
      case _: LoopUntilStart =>
        outlist += xml.Text("\n")
        outlist += xml.Text(indent + "  ")
        outlist += <out-edge output-port="test"></out-edge>
      case _ => ()
    }

    if (inlist.nonEmpty) {
      inlist += xml.Text("\n" + indent)
      nodes += xml.Text(indent)
      nodes += <inputs>{ inlist }</inputs>
    }

    if (outputs.nonEmpty) {
      if (inputs.nonEmpty) {
        nodes += xml.Text("\n")
      }

      outlist += xml.Text("\n" + indent)
      nodes += xml.Text(indent)
      nodes += <outputs>{ outlist }</outputs>
    }

    /*
    this match {
      case _: Binding =>
        if (inputs.nonEmpty) {
          nodes += xml.Text("\n")
        }

        val outlist = ListBuffer.empty[xml.Node]
        for (edge <- graph.edgesFrom(this)) {
          if (edge.fromPort == "result") {
            outlist += xml.Text("\n")
            outlist += xml.Text(indent + "  ")
            outlist += <out-edge output-port={ edge.fromPort } input-port={ edge.toPort } destination={ edge.to.id }></out-edge>
          } else {
            logger.error(s"Binding has output edge named ${edge.fromPort}")
          }
        }
        outlist += xml.Text("\n" + indent)
        nodes += xml.Text(indent)
        nodes += <outputs>{ outlist }</outputs>
      case _ => Unit
    }
     */

    nodes += xml.Text("\n" + indent)
    nodes += dumpClosedChildren(depth)

    // Hack for closing indent
    if (indent.length >= 2) {
      nodes += xml.Text("\n" + indent.substring(2))
    } else {
      nodes += xml.Text("\n")
    }

    val className = if (step.isDefined) {
      step.get.getClass.getName
    } else {
      this.getClass.getName
    }
    val shortName = className.split("\\.").last

    val nodeName = this match {
      case _: ContainerStart => "container"
      case _ => "node"
    }

    // Ba-ar-af. My $DEITY this is ugly.
    val attrs = new xml.UnprefixedAttribute("id", xml.Text(id),
      new UnprefixedAttribute("label", xml.Text(label),
        new UnprefixedAttribute("className", xml.Text(className),
          new UnprefixedAttribute("name", xml.Text(shortName), xml.Null))))
    new xml.Elem(null, nodeName, attrs, xml.TopScope, false, nodes:_*)
  }

  private[graph] def inputsOk(): Boolean
  private[graph] def outputsOk(): Boolean
}
