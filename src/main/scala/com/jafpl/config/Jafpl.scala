package com.jafpl.config

import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.Graph
import com.jafpl.util.{ErrorListener, TraceEventManager}

object Jafpl {
  private val _configProperty = "com.jafpl.config.JafplConfigurer"
  private val _configClass = "com.jafpl.config.DefaultJafplConfigurer"

  def newInstance(): Jafpl = {
    val configurer = Class.forName(configClass).newInstance()
    val config = new Jafpl()
    configurer.asInstanceOf[JafplConfigurer].configure(config)
    config.close()
    config
  }

  private def configClass: String = Option(System.getProperty(_configProperty)).getOrElse(_configClass)
}

class Jafpl {
  private var closed = false
  private var _traceEventManager: TraceEventManager = _
  private var _errorListener: ErrorListener = _

  def traceEventManager: TraceEventManager = {
    if (_traceEventManager == null) {
      throw new PipelineException("unconfig", "attempt to use unconfigured jafpl", None)
    }
    _traceEventManager
  }
  def traceEventManager_=(manager: TraceEventManager): Unit = {
    checkClosed()
    _traceEventManager = manager
  }

  def errorListener: ErrorListener = {
    if (_errorListener == null) {
      throw new PipelineException("unconfig", "attempt to use unconfigured jafpl", None)
    }
    _errorListener
  }
  def errorListener_=(listener: ErrorListener): Unit = {
    checkClosed()
    _errorListener = listener
  }

  def newGraph(): Graph = {
    new Graph(this)
  }

  def close(): Unit = {
    closed = true
  }
  private def checkClosed(): Unit = {
    if (closed) {
      throw new PipelineException("closed", "Cannot change a closed configuration.", None)
    }
  }
}
