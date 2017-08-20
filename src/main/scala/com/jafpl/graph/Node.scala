package com.jafpl.graph

import com.jafpl.exceptions.GraphException
import com.jafpl.steps.Step
import com.jafpl.util.UniqueId

import scala.collection.Set
import scala.collection.mutable.ListBuffer
import scala.xml.UnprefixedAttribute

abstract private[jafpl] class Node(val graph: Graph, val step: Option[Step], val userLabel: Option[String]) {
  private var _start: Option[ContainerStart] = None
  val id: String = UniqueId.nextId.toString

  private val _name: String = if (userLabel.isDefined) {
    val regex = """([\p{L}_][-\p{L}_\p{N}]*)""".r
    userLabel.get match {
      case regex(lbl) => lbl
      case _ => throw new GraphException(s"Invalid label: ${userLabel.get}")
    }
  } else {
    var name = super.toString.split('.').last
    if (name.indexOf('@') > 0) {
      name = name.substring(0, name.indexOf('@'))
    }
    name
  }
  private val _label = s"${_name}-$id"

  def label: String = _label

  override def toString: String = {
    s"{$label}"
  }

  def dependsOn(node: Node): Unit = {
    graph.addDependsEdge(node, this)
  }

  def inputs: Set[String] = graph.inboundPorts(this)
  def outputs: Set[String] = graph.outboundPorts(this)
  def bindings: Set[String] = graph.bindings(this)

  protected[jafpl] def inputEdge(port: String): Edge = {
    graph.edgesTo(this, port).head
  }

  protected[jafpl] def outputEdge(port: String): Edge = {
    graph.edgesFrom(this, port).head
  }

  def parent: Option[ContainerStart] = _start
  def parent_=(node: ContainerStart): Unit = {
    if (_start.isEmpty) {
      _start = Some(node)
    } else {
      throw new GraphException("Parent of " + this + " is already defined: " + _start.get)
    }
  }

  protected[graph] def dumpChildren(depth: Int): xml.Node = {
    xml.Text("")
  }

  protected[graph] def dump(depth: Int): xml.Elem = {
    val indent = " " * depth
    val nodes = ListBuffer.empty[xml.Node]
    nodes += xml.Text("\n")

    if (inputs.nonEmpty || bindings.nonEmpty) {
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

    this match {
      case b: Binding =>
        if (inputs.nonEmpty) {
          nodes += xml.Text("\n")
        }

        val outlist = ListBuffer.empty[xml.Node]
        for (edge <- graph.edgesFrom(this, "result")) {
          outlist += xml.Text("\n")
          outlist += xml.Text(indent + "  ")
          outlist += <out-edge output-port={ edge.fromPort } input-port={ edge.toPort } destination={ edge.to.id }></out-edge>
        }
        outlist += xml.Text("\n" + indent)
        nodes += xml.Text(indent)
        nodes += <outputs>{ outlist }</outputs>
      case _ => Unit
    }

    nodes += xml.Text("\n" + indent)
    nodes += dumpChildren(depth)

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
        new UnprefixedAttribute("end", xml.Text(start.end.id), xml.Null)
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

  def inputsOk(): Boolean
  def outputsOk(): Boolean
}
