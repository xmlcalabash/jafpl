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
  override def getMessage: String = msg

  override def toString: String = {
    var str = ""

    if (location.isDefined) {
      val loc = location.get
      if (loc.uri.isDefined) {
        str = str + loc.uri.get + ":"
      }
      if (loc.line.isDefined) {
        str = str + loc.line.get + ":"
      }
      if (loc.column.isDefined) {
        str = str + loc.column.get + ":"
      }
    }

    str + msg
  }
}
