package com.jafpl.graph

import com.jafpl.exceptions.GraphException
import com.jafpl.steps.{Step, ViewportComposer}

/** A choose container.
  *
  * Choose containers are created with the `addChoose` method of [[com.jafpl.graph.ContainerStart]].
  *
  * @param graph The graph into which this node is to be inserted.
  * @param end The end of this container.
  * @param userLabel An optional user-defined label.
  */
class ChooseStart private[jafpl] (override val graph: Graph,
                                  override protected val end: ContainerEnd,
                                  override val userLabel: Option[String])
  extends ContainerStart(graph, end, userLabel) {

  def addWhen(expression: String, label: String): WhenStart = addWhen(expression, Some(label))
  def addWhen(expression: String, label: Option[String]): WhenStart = {
    val node = graph.addWhen(expression, label)
    addChild(node)
    node
  }

  override def addAtomic(step: Step, label: Option[String]): Node = {
    throw new GraphException("Cannot add Atomic steps to Choose", location)
  }

  override def addGroup(label: Option[String]): ContainerStart = {
    throw new GraphException("Cannot add Group steps to Choose", location)
  }

  override def addChoose(label: Option[String]): ChooseStart = {
    throw new GraphException("Cannot add Choose steps to Choose", location)
  }

  override def addForEach(label: Option[String]): ForEachStart = {
    throw new GraphException("Cannot add For-Each steps to Choose", location)
  }

  override def addViewport(composer: ViewportComposer, label: Option[String]): ViewportStart = {
    throw new GraphException("Cannot add Viewport steps to Choose", location)
  }

  override def addTryCatch(label: Option[String]): TryCatchStart = {
    throw new GraphException("Cannot add Try/Catch steps to Choose", location)
  }
}
