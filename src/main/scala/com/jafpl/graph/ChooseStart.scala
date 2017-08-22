package com.jafpl.graph

import com.jafpl.exceptions.GraphException
import com.jafpl.steps.{Step, ViewportComposer}

private[jafpl] class ChooseStart(override val graph: Graph,
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
    throw new GraphException("Cannot add Atomic steps to Choose")
  }

  override def addGroup(label: Option[String]): ContainerStart = {
    throw new GraphException("Cannot add Group steps to Choose")
  }

  override def addChoose(label: Option[String]): ChooseStart = {
    throw new GraphException("Cannot add Choose steps to Choose")
  }

  override def addForEach(label: Option[String]): ForEachStart = {
    throw new GraphException("Cannot add For-Each steps to Choose")
  }

  override def addViewport(composer: ViewportComposer, label: Option[String]): ViewportStart = {
    throw new GraphException("Cannot add Viewport steps to Choose")
  }

  override def addTryCatch(label: Option[String]): TryCatchStart = {
    throw new GraphException("Cannot add Try/Catch steps to Choose")
  }
}
