package com.jafpl.steps

object PortCardinality {
  val UNBOUNDED = -1
  def ZERO_OR_MORE = new PortCardinality(0)
  def ONE_OR_MORE = new PortCardinality(1)
  def EXACTLY_ONE = new PortCardinality(1, 1)
}

class PortCardinality(private val minimumCardinality: Long, private val maximumCardinality: Option[Long]) {
  require(minimumCardinality >= 0)
  require(maximumCardinality.isEmpty
    || maximumCardinality.get == PortCardinality.UNBOUNDED
    || maximumCardinality.get >= minimumCardinality)

  def this() {
    this(0, None)
  }

  def this(min: Long) {
    this(min, None)
  }

  def this(min: Long, max: Long) {
    this(min, Some(max))
  }

  def minimum: Long = minimumCardinality
  def maximum: Long = {
    if (maximumCardinality.isDefined) {
      maximumCardinality.get
    } else {
    PortCardinality.UNBOUNDED
    }
  }

  def withinBounds(count: Long): Boolean = {
    minimumCardinality <= count && (maximumCardinality.isEmpty || maximumCardinality.get >= count)
  }

  override def toString: String = {
    if (maximumCardinality.isDefined) {
      s"Edge cardinality: [${minimum}-${maximum}]"
    } else {
      s"Edge cardinality: [${minimum}-UNBOUNDED]"
    }
  }
}
