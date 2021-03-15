package com.jafpl.graph

object NodeLocation {
  val UNKNOWN = new NodeLocation(None, None, None)
}

class NodeLocation(locuri: Option[String], locline: Option[Long], loccol: Option[Long]) extends Location {
  def this(uri: String) = {
    this(Some(uri), None, None)
  }

  def this(uri: String, line: Long) = {
    this(Some(uri), Some(line), None)
  }

  def this(uri: String, line: Long, col: Long) = {
    this(Some(uri), Some(line), Some(col))
  }

  def uri: Option[String] = locuri
  def line: Option[Long] = locline
  def column: Option[Long] = loccol
}

