package com.jafpl.graph

import com.jafpl.exceptions.JafplException
import com.jafpl.steps.{Manifold, ManifoldSpecification, Step, ViewportComposer}

class TryCatchStart private[jafpl] (override val graph: Graph,
                                    override protected val end: ContainerEnd,
                                    override val userLabel: Option[String],
                                    private val manspec: ManifoldSpecification)
  extends ContainerStart(graph, end, userLabel) {

  manifold = manspec

  def addTry(label: String, manifold: ManifoldSpecification): TryStart = addTry(Some(label), manifold)

  def addTry(label: Option[String], manifold: ManifoldSpecification): TryStart = {
    val node = graph.addTry(label, manifold)
    addChild(node)
    node
  }

  def addCatch(label: Option[String], codes: List[Any], manifold: ManifoldSpecification): CatchStart = {
    val node = graph.addCatch(label, codes, manifold)
    addChild(node)
    node
  }

  def addCatch(label: String, manifold: ManifoldSpecification): CatchStart = addCatch(Some(label), List(), manifold)
  def addCatch(label: String, code: Any, manifold: ManifoldSpecification): CatchStart = addCatch(Some(label), List(code), manifold)
  def addCatch(label: String, codes: List[Any], manifold: ManifoldSpecification): CatchStart = addCatch(Some(label), codes, manifold)

  def addFinally(label: Option[String], manifold: ManifoldSpecification): FinallyStart = {
    val node = graph.addFinally(label, manifold)
    addChild(node)
    node
  }

  def addFinally(label: String, manifold: ManifoldSpecification): FinallyStart = addFinally(Some(label), manifold)

  override def addAtomic(step: Step, label: Option[String]): Node = {
    throw JafplException.childForbidden(this.toString, step.toString, location)
  }

  override def addGroup(label: Option[String], manifold: ManifoldSpecification): ContainerStart = {
    throw JafplException.childForbidden(this.toString, label.getOrElse("group"), location)
  }

  override def addChoose(label: Option[String]): ChooseStart = {
    throw JafplException.childForbidden(this.toString, label.getOrElse("choose"), location)
  }

  override def addForEach(label: Option[String], manifold: ManifoldSpecification): LoopEachStart = {
    throw JafplException.childForbidden(this.toString, label.getOrElse("for-each"), location)
  }

  override def addViewport(composer: ViewportComposer, label: Option[String], manifold: ManifoldSpecification): ViewportStart = {
    throw JafplException.childForbidden(this.toString, label.getOrElse("viewport"), location)
  }

  override def addTryCatch(label: Option[String], manifold: ManifoldSpecification): TryCatchStart = {
    throw JafplException.childForbidden(this.toString, label.getOrElse("try"), location)
  }
}
