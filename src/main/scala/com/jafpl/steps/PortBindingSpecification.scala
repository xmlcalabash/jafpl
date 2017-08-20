package com.jafpl.steps

import com.jafpl.exceptions.{GraphException, PipelineException}

import scala.collection.{immutable, mutable}

object PortBindingSpecification {
  val ANY: PortBindingSpecification = new PortBindingSpecification(Map("*" -> "*"))
  val NONE: PortBindingSpecification = new PortBindingSpecification(Map())
  val SOURCE: PortBindingSpecification = new PortBindingSpecification(Map("source" -> "1"))
  val RESULT: PortBindingSpecification = new PortBindingSpecification(Map("result" -> "1"))
  val SOURCESEQ: PortBindingSpecification = new PortBindingSpecification(Map("source" -> "*"))
  val RESULTSEQ: PortBindingSpecification = new PortBindingSpecification(Map("result" -> "*"))
}

class PortBindingSpecification(spec: immutable.Map[String,String]) {
  private val cardinalities: List[String] = List("1", "?", "+", "*")
  for (port <- spec.keySet) {
    if (!cardinalities.contains(spec(port))) {
      throw new GraphException(s"Invalid cardinality for port $port: ${spec(port)}")
    }
    if ((port == "*") && (spec(port) != "*")) {
      throw new GraphException(s"Only cardinality '*' is allowed for the wildcard port")
    }
  }

  def ports(): Set[String] = {
    val plist = mutable.HashSet.empty[String]
    for (port <- spec.keySet) {
      if (port != "*") {
        plist += port
      }
    }
    plist.toSet
  }

  def checkCardinality(port: String, count: Long): Unit = {
    if (count < 0) {
      throw new PipelineException("badcount", s"Impossible document count '$count' for port '$port'")
    }

    var pass = false
    if (spec.contains(port)) {
      if (count == 0) {
        pass = (spec(port) == "?") || (spec(port) == "*")
      } else if (count == 1) {
        pass = (spec(port) == "?") || (spec(port) == "1")
      }  else {
        pass = (spec(port) == "+") || (spec(port) == "*")
      }

      if (!pass) {
        throw new PipelineException("badcardinality",
          s"Cardinality error: '$count' document(s) sent to port '$port' (allowed: ${spec(port)})")
      }
    } else {
      if (!spec.contains("*")) {
        throw new PipelineException("badport", s"Port named '$port' is not allowed")
      }
    }
  }
}
