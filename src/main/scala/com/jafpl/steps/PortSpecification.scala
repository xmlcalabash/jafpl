package com.jafpl.steps

import com.jafpl.exceptions.JafplException

import scala.collection.{immutable, mutable}

/** Useful default port binding specifications.
  *
  */
object PortSpecification {
  /** The wildcard port name */
  val WILDCARD: String = "*"
  /** Allow any ports. */
  val ANY: PortSpecification = new PortSpecification(Map(WILDCARD->PortCardinality.ZERO_OR_MORE))
  /** Allow no ports. */
  val NONE: PortSpecification = new PortSpecification(Map())
  /** Allow a single document on the `source` port. */
  val SOURCE: PortSpecification = new PortSpecification(Map("source" -> PortCardinality.EXACTLY_ONE))
  /** Allow a single document on the `result` port. */
  val RESULT: PortSpecification = new PortSpecification(Map("result" -> PortCardinality.EXACTLY_ONE))
  /** Allow a sequence of zero or more documents on the `source` port. */
  val SOURCESEQ: PortSpecification = new PortSpecification(Map("source" -> PortCardinality.ZERO_OR_MORE))
  /** Allow a sequence of zero or more documents on the `result` port. */
  val RESULTSEQ: PortSpecification = new PortSpecification(Map("result" -> PortCardinality.ZERO_OR_MORE))
}

/** Specify the valid bindings for step inputs or outputs
  *
  * The core engine doesn't care what steps are connected or what ports they're connected on.
  * But in practice, many steps do care. If you have a step that expects to read a single
  * document off the `source` port and to write a sequence of documents to the `result` port,
  * it does no good to run a pipeline where those ports aren't connected.
  *
  * A port binding specification identifies the ports that the step expects to read from (or
  * write to) and what the acceptable cardinality of documents is on those ports.
  *
  * The port named `PortSpecification.WILDCARD` serves as a wildcard for ports of any name
  *
  * @constructor A port binding specification.
  * @param spec A map from port names to cardinalities.
  */
class PortSpecification(spec: immutable.Map[String,PortCardinality]) {
  /** List the ports in this specification.
    *
    * @return the set of ports
    */
  def ports: Set[String] = {
    spec.keySet.filter(_ != PortSpecification.WILDCARD)
  }

  def cardinality(port: String): Option[PortCardinality] = {
    if (spec.contains(port)) {
      Some(spec(port))
    } else if (spec.contains(PortSpecification.WILDCARD)) {
      Some(spec(PortSpecification.WILDCARD))
    } else {
      None
    }
  }

  /** Check the actual input cardinality against the specification.
    *
    * This method throws a [[com.jafpl.exceptions.JafplException]] if the `count` of documents
    * provided on this port is not acceptable according to the defined cardinality.
    *
    * @param port The port name.
    * @param count The number of documents that appeared on that port.
    */
  def checkInputCardinality(port: String, count: Long): Unit = {
    checkCardinality(port, count, true)
  }

  /** Check the actual output cardinality against the specification.
    *
    * This method throws a [[com.jafpl.exceptions.JafplException]] if the `count` of documents
    * provided on this port is not acceptable according to the defined cardinality.
    *
    * @param port The port name.
    * @param count The number of documents that appeared on that port.
    */
  def checkOutputCardinality(port: String, count: Long): Unit = {
    checkCardinality(port, count, false)
  }

  private def checkCardinality(port: String, count: Long, input: Boolean): Unit = {
    if (count < 0) {
      JafplException.internalError(s"Impossible document count $count for $port", None)
    }

    val card = cardinality(port)
    if (card.isDefined) {
      //println(s"$count: $card")
      if (!card.get.withinBounds(count)) {
        if (input) {
          throw JafplException.inputCardinalityError(port, count.toString, card.get)
        } else {
          throw JafplException.outputCardinalityError(port, count.toString, card.get)
        }
      }
    } else {
      throw JafplException.badPort(port)
    }
  }
}
