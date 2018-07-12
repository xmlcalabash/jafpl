package com.jafpl.graph

import com.jafpl.exceptions.JafplException
import com.jafpl.steps.{Step, ViewportComposer}

class TryCatchStart private[jafpl] (override val graph: Graph,
                                    override protected val end: ContainerEnd,
                                    override val userLabel: Option[String])
  extends ContainerStart(graph, end, userLabel) {

  def addTry(label: String): TryStart = addTry(Some(label))

  def addTry(label: Option[String]): TryStart = {
    val node = graph.addTry(label)
    addChild(node)
    node
  }

  def addCatch(label: Option[String], codes: List[Any]): CatchStart = {
    val node = graph.addCatch(label, codes)
    addChild(node)
    node
  }

  def addCatch(label: String): CatchStart = addCatch(Some(label), List())
  def addCatch(label: String, code: Any): CatchStart = addCatch(Some(label), List(code))
  def addCatch(label: String, codes: List[Any]): CatchStart = addCatch(Some(label), codes)

  def addFinally(label: Option[String]): FinallyStart = {
    val node = graph.addFinally(label)
    addChild(node)
    node
  }

  def addFinally(label: String): FinallyStart = addFinally(Some(label))

  override def addAtomic(step: Step, label: Option[String]): Node = {
    throw JafplException.childForbidden(this.toString, step.toString, location)
  }

  override def addGroup(label: Option[String]): ContainerStart = {
    throw JafplException.childForbidden(this.toString, label.getOrElse("group"), location)
  }

  override def addChoose(label: Option[String]): ChooseStart = {
    throw JafplException.childForbidden(this.toString, label.getOrElse("choose"), location)
  }

  override def addForEach(label: Option[String]): LoopEachStart = {
    throw JafplException.childForbidden(this.toString, label.getOrElse("for-each"), location)
  }

  override def addViewport(composer: ViewportComposer, label: Option[String]): ViewportStart = {
    throw JafplException.childForbidden(this.toString, label.getOrElse("viewport"), location)
  }

  override def addTryCatch(label: Option[String]): TryCatchStart = {
    throw JafplException.childForbidden(this.toString, label.getOrElse("try"), location)
  }
}
