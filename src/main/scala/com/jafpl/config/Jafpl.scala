package com.jafpl.config

import com.jafpl.exceptions.JafplException
import com.jafpl.graph.Graph
import com.jafpl.sbt.BuildInfo
import com.jafpl.util.{ErrorListener, TraceEventManager}

/** Just Another Fine Pipeline Language.
  *
  * This object is the factory for new instances of Jafpl.
  *
  */
object Jafpl {
  private val _configProperty = "com.jafpl.config.JafplConfigurer"
  private val _configClass = "com.jafpl.config.DefaultJafplConfigurer"

  /** Create a new instance of Jafpl.
    *
    * This method attempts to instantiate a class to configure Jafpl using the system property
    * name `com.jafpl.config.JafplConfigurer`. If no such property exists, the default configurer
    * is instantiated.
    *
    * A new Jafpl instance is the instantiated and passed to the configurer. After configuration,
    * the instance is closed; attempting to update the properties will raise an exception.
    *
    * @return Your new, configured pipeline instance.
    */
  def newInstance(): Jafpl = {
    val configurer = Class.forName(configClass).getDeclaredConstructor().newInstance()
    val config = new Jafpl()
    configurer.asInstanceOf[JafplConfigurer].configure(config)
    config.close()
    config
  }

  private def configClass: String = Option(System.getProperty(_configProperty)).getOrElse(_configClass)
}

/** An instance of Just Another Fine Pipeline Language.
  *
  * @constructor Construct a new Jafpl instance (private; use [[com.jafpl.config.Jafpl.newInstance()]])
  */
class Jafpl private() {
  private var closed = false
  private var _traceEventManager: TraceEventManager = _
  private var _errorListener: ErrorListener = _

  def version: String = BuildInfo.version
  def gitHash: String = BuildInfo.gitHash

  /** Return the trace event manager.
    *
    * @return The trace event manager.
    * @throws JafplException if there is no [[com.jafpl.util.TraceEventManager]].
    */
  def traceEventManager: TraceEventManager = {
    if (_traceEventManager == null) {
      throw JafplException.unconfigured("traceEventManager")
    }
    _traceEventManager
  }

  /** Assign a new trace event manager. */
  def traceEventManager_=(manager: TraceEventManager): Unit = {
    checkClosed()
    _traceEventManager = manager
  }

  /** Return the error listener.
    *
    * @return The error listener.
    * @throws JafplException if there is no [[com.jafpl.util.TraceEventManager]].
    */
  def errorListener: ErrorListener = {
    if (_errorListener == null) {
      throw JafplException.unconfigured("errorListener")
    }
    _errorListener
  }

  /** Assign a new error listener */
  def errorListener_=(listener: ErrorListener): Unit = {
    checkClosed()
    _errorListener = listener
  }

  /** Create a new graph. */
  def newGraph(): Graph = {
    new Graph(this)
  }

  /** Close the instance.
    *
    * A close instance cannot be changed.
    */
  def close(): Unit = {
    closed = true
  }

  private def checkClosed(): Unit = {
    if (closed) {
      throw JafplException.configurationClosed()
    }
  }
}
