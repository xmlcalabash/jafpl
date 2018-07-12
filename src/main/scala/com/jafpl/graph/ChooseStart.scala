package com.jafpl.graph

import com.jafpl.exceptions.JafplException
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

  def addWhen(expression: Any, label: String): WhenStart = addWhen(expression, Some(label))
  def addWhen(expression: Any, label: Option[String]): WhenStart = {
    val node = graph.addWhen(expression, label)
    addChild(node)
    node
  }

  override def addAtomic(step: Step, label: Option[String]): Node = {
    throw JafplException.childForbidden(this.label, label.getOrElse(step.toString), location)
  }

  override def addGroup(label: Option[String]): ContainerStart = {
    throw JafplException.childForbidden(this.label, label.getOrElse("group"), location)
  }

  override def addChoose(label: Option[String]): ChooseStart = {
    throw JafplException.childForbidden(this.label, label.getOrElse("choose"), location)
  }

  override def addForEach(label: Option[String]): LoopEachStart = {
    throw JafplException.childForbidden(this.label, label.getOrElse("for-each"), location)
  }

  override def addViewport(composer: ViewportComposer, label: Option[String]): ViewportStart = {
    throw JafplException.childForbidden(this.label, label.getOrElse("viewport"), location)
  }

  override def addTryCatch(label: Option[String]): TryCatchStart = {
    throw JafplException.childForbidden(this.label, label.getOrElse("try"), location)
  }
}
