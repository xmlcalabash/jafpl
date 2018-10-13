package com.jafpl.steps

object Manifold {
  def WILD = new PortSpecification(Map(PortSpecification.WILDCARD -> PortCardinality.ZERO_OR_MORE))
  def ALLOW_ANY = new Manifold(WILD, WILD)
  def singlePort(port: String, cardinality: PortCardinality): PortSpecification = {
    new PortSpecification(Map(port->cardinality))
  }
  def singlePort(port: String, minimum: Long, maximum: Long): PortSpecification = {
    new PortSpecification(Map(port->new PortCardinality(minimum, maximum)))
  }
}

class Manifold(private val input: PortSpecification, private val output: PortSpecification) extends ManifoldSpecification {
  override def inputSpec: PortSpecification = input
  override def outputSpec: PortSpecification = output
}
