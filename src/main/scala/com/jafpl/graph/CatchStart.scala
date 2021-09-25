package com.jafpl.graph

import com.jafpl.steps.{Manifold, ManifoldSpecification, Step}

/** The catch block of a try catch.
  *
  * Catch containers are created with the `addCatch` method of [[com.jafpl.graph.TryCatchStart]].
  *
  * @param graph The graph into which this node is to be inserted.
  * @param end The end of this container.
  * @param userLabel An optional user-defined label.
  * @param codes The codes caught by this catch.
  */
class CatchStart private[jafpl] (override val graph: Graph,
                                 override protected val end: ContainerEnd,
                                 override val userLabel: Option[String],
                                 val codes: List[Any],
                                 private val manspec: ManifoldSpecification) extends ContainerStart(graph, end, userLabel) {
  protected[jafpl] var cause = Option.empty[Throwable]
  manifold = manspec

  private var _translator = Option.empty[Step]
  def translator: Option[Step] = _translator
  def translator_=(xlate: Step): Unit = {
    if (_translator.isDefined) {
      throw new RuntimeException("Translator already defined")
    }
    _translator = Some(xlate)
  }
}
