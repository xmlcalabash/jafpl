package com.jafpl.graph

import com.jafpl.exceptions.GraphException
import com.jafpl.steps.{Step, ViewportComposer}

private[jafpl] class TryCatchStart(override val graph: Graph,
                                   override protected val end: ContainerEnd,
                                   override val userLabel: Option[String])
  extends ContainerStart(graph, end, userLabel) {

  def addTry(label: String): TryStart = addTry(Some(label))

  def addTry(label: Option[String]): TryStart = {
    val node = graph.addTry(label)
    addChild(node)
    node
  }

  def addCatch(label: Option[String], codes: List[String]): CatchStart = {
    val node = graph.addCatch(label, codes)
    addChild(node)
    node
  }

  def addCatch(label: String): CatchStart = addCatch(None, List())
  def addCatch(label: String, code: String): CatchStart = addCatch(None, List(code))
  def addCatch(label: String, codes: List[String]): CatchStart = addCatch(None, codes)

  override def addAtomic(step: Step, label: Option[String]): Node = {
    throw new GraphException("Cannot add Atomic steps to Try/Catch", location)
  }

  override def addGroup(label: Option[String]): ContainerStart = {
    throw new GraphException("Cannot add Group steps to Try/Catch", location)
  }

  override def addChoose(label: Option[String]): ChooseStart = {
    throw new GraphException("Cannot add Choose steps to Try/Catch", location)
  }

  override def addForEach(label: Option[String]): ForEachStart = {
    throw new GraphException("Cannot add For-Each steps to Try/Catch", location)
  }

  override def addViewport(composer: ViewportComposer, label: Option[String]): ViewportStart = {
    throw new GraphException("Cannot add Viewport steps to Try/Catch", location)
  }

  override def addTryCatch(label: Option[String]): TryCatchStart = {
    throw new GraphException("Cannot add Try/Catch steps to Try/Catch", location)
  }
}
