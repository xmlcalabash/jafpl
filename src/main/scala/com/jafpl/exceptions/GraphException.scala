package com.jafpl.exceptions

import com.jafpl.graph.Location

/** An exception raised by an illegal graph.
  *
  * Attempts to construct an illegal graph (circular references, edges between nodes
  * in different graphs, etc.) raise this exception.
  *
  * @constructor A graph exception.
  * @param msg A message that describes the exception condition.
  */
class GraphException(val msg: String, val location: Option[Location]) extends RuntimeException {
  def this(msg: String) {
    this(msg, None)
  }
}
