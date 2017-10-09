package com.jafpl.steps

import com.jafpl.exceptions.{GraphException, PipelineException}

import scala.collection.{immutable, mutable}

/** Useful default port binding specifications.
  *
  */
object PortSpecification {
  /** Allow any ports. */
  val ANY: PortSpecification = new PortSpecification(Map("*" -> "*"))
  /** Allow no ports. */
  val NONE: PortSpecification = new PortSpecification(Map())
  /** Allow a single document on the `source` port. */
  val SOURCE: PortSpecification = new PortSpecification(Map("source" -> "1"))
  /** Allow a single document on the `result` port. */
  val RESULT: PortSpecification = new PortSpecification(Map("result" -> "1"))
  /** Allow a sequence of zero or more documents on the `source` port. */
  val SOURCESEQ: PortSpecification = new PortSpecification(Map("source" -> "*"))
  /** Allow a sequence of zero or more documents on the `result` port. */
  val RESULTSEQ: PortSpecification = new PortSpecification(Map("result" -> "*"))
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
  * The available cardinalities are:
  * 1. `1`: exactly one document is required.
  * 2. `?`: one document is expected, but it's optional.
  * 3. `+`: at least one document is expected, but more are ok.
  * 4. `*`: any number of documents are expected, including none.
  *
  * It would, of course, be possible to extend this specification to allow for "exactly 3 documents"
  * or "between 4 and 7 documents", but that seems beyond the point of diminishing returns.
  * The pipeline engine will enforce the cardinalities above, you're free to enforce other
  * cardinalities in the step implementation.
  *
  * The special port name "*" may be given to indicate that other, arbitrarily named ports
  * are also acceptable.
  *
  * @constructor A port binding specification.
  * @param spec A map from port names to cardinalities.
  */
class PortSpecification(spec: immutable.Map[String,String]) {
  private val cardinalities: List[String] = List("1", "?", "+", "*")
  for (port <- spec.keySet) {
    if (!cardinalities.contains(spec(port))) {
      throw new GraphException(s"Invalid cardinality for port $port: ${spec(port)}", None)
    }
    if ((port == "*") && (spec(port) != "*")) {
      throw new GraphException(s"Only cardinality '*' is allowed for the wildcard port", None)
    }
  }

  /** List the ports in this specification.
    *
    * The wildcard port, "*" is not returned in this list.
    *
    * @return
    */
  def ports: Set[String] = {
    val plist = mutable.HashSet.empty[String]
    for (port <- spec.keySet) {
      if (port != "*") {
        plist += port
      }
    }
    plist.toSet
  }

  def cardinality(port: String): Option[String] = {
    if (spec.contains(port)) {
      Some(spec(port))
    } else if (spec.contains("*")) {
      Some(spec("*"))
    } else {
      None
    }
  }

  /** Check the actual cardinality against the specification.
    *
    * This method throws a [[com.jafpl.exceptions.PipelineException]] if the `count` of documents
    * provided on this port is not acceptable according to the defined cardinality.
    *
    * @param port The port name.
    * @param count The number of documents that appeared on that port.
    */
  def checkCardinality(port: String, count: Long): Unit = {
    if (count < 0) {
      throw new PipelineException("badcount", s"Impossible document count '$count' for port '$port'", None)
    }

    var pass = false
    if (spec.contains(port)) {
      if (spec(port) == "*") {
        pass = true
      } else {
        pass = count match {
          case 0 => spec(port) == "?"
          case 1 => (spec(port) == "?") || (spec(port) == "1")
          case _ => spec(port) == "+"
        }
      }

      if (!pass) {
        throw new PipelineException("badcardinality",
          s"Cardinality error: '$count' document(s) sent to port '$port' (allowed: ${spec(port)})", None)
      }
    } else {
      if (!spec.contains("*")) {
        throw new PipelineException("badport", s"Port named '$port' is not allowed", None)
      }
    }
  }
}
