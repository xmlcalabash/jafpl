package com.jafpl.graph

import com.jafpl.steps.{Step, ViewportComposer}

import scala.collection.mutable.ListBuffer

private[jafpl] class ContainerStart(override val graph: Graph,
                                    val end: ContainerEnd,
                                    override val userLabel: Option[String]) extends Node(graph, None, userLabel) {
  private val _children = ListBuffer.empty[Node]

  def children: List[Node] = _children.toList

  override def inputsOk(): Boolean = {
    if (inputs.nonEmpty) {
      var valid = true
      for (port <- inputs) {
        if (port != "#bindings") {
          println("Invalid binding on " + this + ": " + port)
          valid = false
        }
      }
      valid
    } else {
      true
    }
  }

  override def outputsOk() = true

  protected[graph] def addChild(node: Node): Unit = {
    node.parent = this
    _children += node
  }

  def addAtomic(step: Step): Node = addAtomic(step, None)

  def addAtomic(step: Step, label: String): Node = addAtomic(step, Some(label))

  def addAtomic(step: Step, label: Option[String]): Node = {
    val node = graph.addAtomic(step, label)
    addChild(node)
    node
  }

  def addGroup(): ContainerStart = addGroup(None)

  def addGroup(label: String): ContainerStart = addGroup(Some(label))

  def addGroup(label: Option[String]): ContainerStart = {
    val node = graph.addGroup(label)
    addChild(node)
    node
  }

  def addChoose(): ChooseStart = addChoose(None)

  def addChoose(label: String): ChooseStart = addChoose(Some(label))

  def addChoose(label: Option[String]): ChooseStart = {
    val node = graph.addChoose(label)
    addChild(node)
    node
  }

  def addForEach(): ForEachStart = addForEach(None)

  def addForEach(label: String): ForEachStart = addForEach(Some(label))

  def addForEach(label: Option[String]): ForEachStart = {
    val node = graph.addForEach(label)
    addChild(node)
    node
  }

  def addViewport(composer: ViewportComposer): ViewportStart = addViewport(composer, None)

  def addViewport(composer: ViewportComposer, label: String): ViewportStart = addViewport(composer, Some(label))

  def addViewport(composer: ViewportComposer, label: Option[String]): ViewportStart = {
    val node = graph.addViewport(composer, label)
    addChild(node)
    node
  }

  def addTryCatch(): TryCatchStart = addTryCatch(None)

  def addTryCatch(label: String): TryCatchStart = addTryCatch(Some(label))

  def addTryCatch(label: Option[String]): TryCatchStart = {
    val node = graph.addTryCatch(label)
    addChild(node)
    node
  }

  def addBinding(name: String, expression: String): Binding = {
    val binding = graph.addBinding(name, expression)
    addChild(binding)
    binding
  }

  protected[graph] def addSplitter(): Splitter = {
    val node = graph.addSplitter()
    addChild(node)
    node
  }

  protected[graph] def addJoiner(): Joiner = {
    val node = graph.addJoiner()
    addChild(node)
    node
  }

  override protected[graph] def dumpChildren(depth: Int): xml.Node = {
    val indent = " " * depth
    val nodes = ListBuffer.empty[xml.Node]
    for (node <- children) {
      nodes += xml.Text("\n" + indent)
      nodes += node.dump(depth + 2)
    }
    nodes += xml.Text("\n" + indent)
    nodes += end.dump(depth + 2)

    // Hack for closing indent
    if (depth >= 2) {
      nodes += xml.Text("\n" + (" " * (depth - 2)))
    } else {
      nodes += xml.Text("\n")
    }

    <children>{ nodes }</children>
  }
}
