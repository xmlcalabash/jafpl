package com.jafpl.graph

import com.jafpl.graph.JoinMode.JoinMode
import com.jafpl.steps.{Step, ViewportComposer}
import com.jafpl.util.{ItemComparator, ItemTester}

import scala.collection.mutable.ListBuffer

/** A node that contains other nodes.
  *
  * Conceptually, some nodes contain others. A loop, for example, contains the steps that form the
  * body of the loop.
  *
  * In practice, containers are represented by a start and an end.
  *
  * @constructor A container in the pipeline graph.
  * @param graph The graph into which this node is to be inserted.
  * @param end The end of this container.
  * @param userLabel An optional user-defined label.
  */
class ContainerStart protected[jafpl] (override val graph: Graph,
                                       protected val end: ContainerEnd,
                                       override val userLabel: Option[String])
  extends Node(graph, None, userLabel) {

  private val _children = ListBuffer.empty[Node]

  /** The children of this container. */
  def children: List[Node] = _children.toList

  protected[jafpl] def containerEnd: ContainerEnd = end

  private[graph] override def inputsOk(): Boolean = {
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

  private[graph] override def outputsOk() = true

  protected[graph] def addChild(node: Node): Unit = {
    node.parent = this
    _children += node
  }

  /** Add a new atomic step to this container.
    *
    * @param step The step implementation.
    * @return The node added.
    */
  def addAtomic(step: Step): Node = addAtomic(step, None)

  /** Add a new atomic step to this container.
    *
    * @param step The step implementation.
    * @param label A user-defined label.
    * @return The node added.
    */
  def addAtomic(step: Step, label: String): Node = addAtomic(step, Some(label))

  /** Add a new atomic step to this container.
    *
    * @param step The step implementation.
    * @param label An optional, user-defined label.
    * @return The node added.
    */
  def addAtomic(step: Step, label: Option[String]): Node = {
    val node = graph.addAtomic(step, label)
    addChild(node)
    node
  }

  /** Add a new group container to this container.
    *
    * @return The node added.
    */
  def addGroup(): ContainerStart = addGroup(None)

  /** Add a new group container to this container.
    *
    * @param label A user-defined label.
    * @return The node added.
    */
  def addGroup(label: String): ContainerStart = addGroup(Some(label))

  /** Add a new group container to this container.
    *
    * @param label An optional, user-defined label.
    * @return The node added.
    */
  def addGroup(label: Option[String]): ContainerStart = {
    val node = graph.addGroup(label)
    addChild(node)
    node
  }

  /** Add a new choose/when container to this container.
    *
    * @return The node added.
    */
  def addChoose(): ChooseStart = addChoose(None)

  /** Add a new choose/when container to this container.
    *
    * @param label A user-defined label.
    * @return The node added.
    */
  def addChoose(label: String): ChooseStart = addChoose(Some(label))

  /** Add a new choose/when container to this container.
    *
    * @param label An optional, user-defined label.
    * @return The node added.
    */
  def addChoose(label: Option[String]): ChooseStart = {
    val node = graph.addChoose(label)
    addChild(node)
    node
  }

  /** Add a new for-each container to this container.
    *
    * @return The node added.
    */
  def addForEach(): LoopEachStart = addForEach(None)

  /** Add a new for-each container to this container.
    *
    * @param label A user-defined label.
    * @return The node added.
    */
  def addForEach(label: String): LoopEachStart = addForEach(Some(label))

  /** Add a new for-each container to this container.
    *
    * @param label An optional, user-defined label.
    * @return The node added.
    */
  def addForEach(label: Option[String]): LoopEachStart = {
    val node = graph.addForEach(label)
    addChild(node)
    node
  }

  /** Add a new for-loop container to this container.
    *
    * @return The node added.
    */
  def addFor(countTo: Long): LoopForStart = addFor(None, 1, countTo, 1)

  /** Add a new for-loop container to this container.
    *
    * @param label A user-defined label.
    * @return The node added.
    */
  def addFor(label: String, countTo: Long): LoopForStart = addFor(Some(label), 1, countTo, 1)

  def addFor(countFrom: Long, countTo: Long): LoopForStart = addFor(None, countFrom, countTo, 1)
  def addFor(label: String, countFrom: Long, countTo: Long): LoopForStart = addFor(Some(label), countFrom, countTo, 1)
  def addFor(countFrom: Long, countTo: Long, countBy: Long): LoopForStart = addFor(None, countFrom, countTo, countBy)
  def addFor(label: String, countFrom: Long, countTo: Long, countBy: Long): LoopForStart = addFor(Some(label), countFrom, countTo, countBy)

  /** Add a new for-loop container to this container.
    *
    * @param label An optional, user-defined label.
    * @return The node added.
    */
  def addFor(label: Option[String], countFrom: Long, countTo: Long, countBy: Long): LoopForStart = {
    val node = graph.addFor(label, countFrom, countTo, countBy)
    addChild(node)
    node
  }

  /** Add a new while container to this container.
    *
    * @param tester The test evaluator.
    * @return The node added.
    */
  def addWhile(tester: ItemTester): LoopWhileStart = addWhile(tester, None)

  /** Add a new while container to this container.
    *
    * @param tester The test evaluator.
    * @param label A user-defined label.
    * @return The node added.
    */
  def addWhile(tester: ItemTester, label: String): LoopWhileStart = addWhile(tester, Some(label))

  /** Add a new while container to this container.
    *
    * @param tester The test evaluator.
    * @param label An optional, user-defined label.
    * @return The node added.
    */
  def addWhile(tester: ItemTester, label: Option[String]): LoopWhileStart = {
    val node = graph.addWhile(tester, label)
    addChild(node)
    node
  }

  /** Add a new until container to this container.
    *
    * @param comparator The comparator.
    * @return The node added.
    */
  def addUntil(comparator: ItemComparator): LoopUntilStart = addUntil(comparator, None)

  /** Add a new until container to this container.
    *
    * @param comparator The comparator.
    * @param label A user-defined label.
    * @return The node added.
    */
  def addUntil(comparator: ItemComparator, label: String): LoopUntilStart =
    addUntil(comparator, Some(label))

  /** Add a new until container to this container.
    *
    * @param comparator The comparator.
    * @param label An optional, user-defined label.
    * @return The node added.
    */
  def addUntil(comparator: ItemComparator, label: Option[String]): LoopUntilStart = {
    val node = graph.addUntil(comparator, label)
    addChild(node)
    node
  }

  /** Add a new viewport container to this container.
    *
    * @return The node added.
    */
  def addViewport(composer: ViewportComposer): ViewportStart = addViewport(composer, None)

  /** Add a new viewport container to this container.
    *
    * @param label A user-defined label.
    * @return The node added.
    */
  def addViewport(composer: ViewportComposer, label: String): ViewportStart = addViewport(composer, Some(label))

  /** Add a new viewport container to this container.
    *
    * @param label An optional, user-defined label.
    * @return The node added.
    */
  def addViewport(composer: ViewportComposer, label: Option[String]): ViewportStart = {
    val node = graph.addViewport(composer, label)
    addChild(node)
    node
  }

  /** Add a new try/catch container to this container.
    *
    * @return The node added.
    */
  def addTryCatch(): TryCatchStart = addTryCatch(None)

  /** Add a new try/catch container to this container.
    *
    * @param label A user-defined label.
    * @return The node added.
    */
  def addTryCatch(label: String): TryCatchStart = addTryCatch(Some(label))

  /** Add a new try/catch container to this container.
    *
    * @param label An optional, user-defined label.
    * @return The node added.
    */
  def addTryCatch(label: Option[String]): TryCatchStart = {
    val node = graph.addTryCatch(label)
    addChild(node)
    node
  }

  /** Add a option to this container.
    *
    * This method inserts an option binding into the container. This binding is
    * effectively the source of a options's value. Other steps may connect to this
    * binding in order to read its computed value at runtime.
    *
    * Option bindings consist of a name and an expression. The name and expression are
    * arbitrary. At runtime the [[com.jafpl.runtime.ExpressionEvaluator]] provided as
    * part of the [[com.jafpl.runtime.GraphRuntime]] must understand how to evaluate
    * the expression.
    *
    * Unlike a variable, an option value can be supplied at runtime which will
    * be used *instead* of evaluating the expression.
    *
    * At runtime, the computed values are provided to steps through binding edges.
    *
    * @return The node added.
    */
  def addOption(name: String, expression: Any): Binding = {
    val binding = graph.addOption(name, expression, None)
    addChild(binding)
    binding
  }

  def addOption(name: String, expression: Any, options: Any): Binding = {
    val binding = graph.addOption(name, expression, Some(options))
    addChild(binding)
    binding
  }

  /** Add a variable to this container.
    *
    * This method inserts a variable binding into the container. This binding is
    * effectively the source of a variable's value. Other steps may connect to this
    * binding in order to read its computed value at runtime.
    *
    * Variable bindings consist of a name and an expression. The name and expression are
    * arbitrary. At runtime the [[com.jafpl.runtime.ExpressionEvaluator]] provided as
    * part of the [[com.jafpl.runtime.GraphRuntime]] must understand how to evaluate
    * the expression.
    *
    * At runtime, the computed values are provided to steps through binding edges.
    *
    * @return The node added.
    */
  def addVariable(name: String, expression: Any): Binding = {
    val binding = graph.addVariable(name, expression, None, None)
    addChild(binding)
    binding
  }

  def addVariable(name: String, expression: Any, staticValue: Option[Any]): Binding = {
    val binding = graph.addVariable(name, expression, staticValue, None)
    addChild(binding)
    binding
  }

  def addVariable(name: String, expression: Any, staticValue: Option[Any], options: Any): Binding = {
    val binding = graph.addVariable(name, expression, staticValue, Some(options))
    addChild(binding)
    binding
  }

  protected[graph] def addSplitter(): Splitter = {
    val node = graph.addSplitter()
    addChild(node)
    node
  }

  protected[graph] def addJoiner(mode: JoinMode): Joiner = {
    val node = graph.addJoiner(mode)
    addChild(node)
    node
  }

  protected[graph] def addSink(): Sink = {
    val node = graph.addSink()
    addChild(node)
    node
  }

  protected[graph] def addEmptySource(): EmptySource = {
    val node = graph.addEmptySource()
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
