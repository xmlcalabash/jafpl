package com.jafpl.primitive

import com.jafpl.runtime.{ExpressionEvaluator, RuntimeConfiguration}

import scala.collection.mutable

class PrimitiveRuntimeConfiguration(val traceAll: Boolean) extends RuntimeConfiguration() {
  private val evaluator = new PrimitiveExpressionEvaluator()
  private val enabledTraces = mutable.HashSet.empty[String]
  private val disabledTraces = mutable.HashSet.empty[String]

  def this() {
    this(false)
  }

  private val prop = Option(System.getProperty("com.xmlcalabash.trace"))
  if (prop.isDefined) {
    for (trace <- prop.get.split(",").map(_.trim)) {
      var event = trace
      var enable = true

      if (trace.startsWith("-")) {
        event = trace.substring(1)
        enable = false
      } else {
        if (trace.startsWith("+")) {
          event = trace.substring(1)
        }
      }

      if (enable) {
        enabledTraces += event
      } else {
        disabledTraces += event
      }
    }
  }

  override def expressionEvaluator(): ExpressionEvaluator = evaluator

  override def traceEnabled(trace: String): Boolean = {
    if (enabledTraces.contains("ALL")) {
      traceAll || !disabledTraces.contains(trace)
    } else {
      traceAll || enabledTraces.contains("ALL") || enabledTraces.contains(trace)
    }
  }

  override def watchdogTimeout: Long = {
    var timeout: Long = 1000
    val prop = Option(System.getProperty("com.xmlcalabash.watchdogTimeout"))
    if (prop.isDefined) {
      timeout = prop.get.toLong
    }
    timeout
  }
}
