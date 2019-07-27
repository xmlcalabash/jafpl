package com.jafpl.graph

import com.jafpl.steps.PortCardinality

import scala.collection.mutable

/** A looping container.
  *
  * Loops need buffers when they read from steps outside the loop.
  *
  * In practice, containers are represented by a start and an end.
  *
  * @constructor A container in the pipeline graph.
  * @param graph The graph into which this node is to be inserted.
  * @param end The end of this container.
  * @param userLabel An optional user-defined label.
  */
abstract class LoopStart(override val graph: Graph,
                         override protected val end: ContainerEnd,
                         override val userLabel: Option[String]) extends ContainerStart(graph, end, userLabel) {
  private var _iterationSize = 0L
  private var _iterationPosition = 0L

  def iterationSize: Long = _iterationSize
  protected[jafpl] def iterationSize_=(size: Long): Unit = {
    _iterationSize = size
  }

  def iterationPosition: Long = _iterationPosition
  protected[jafpl] def iterationPosition_=(pos: Long): Unit = {
    _iterationPosition = pos
  }
}
