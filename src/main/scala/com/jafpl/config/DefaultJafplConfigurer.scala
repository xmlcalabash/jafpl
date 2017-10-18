package com.jafpl.config

import com.jafpl.util.{DefaultErrorListener, DefaultTraceEventManager}

/** The default [[com.jafpl.config.Jafpl]] configurer.
  *
  * @constructor Create a new instance of the configurer.
  */
class DefaultJafplConfigurer extends JafplConfigurer {
  /** Configure the Jafpl instance.
    *
    * The default configurer uses [[com.jafpl.util.DefaultTraceEventManager]] as the trace
    * event manager and
    * [[com.jafpl.util.DefaultErrorListener]] as the error listener.
    *
    * @param jafpl The Jafpl object that shall be configured.
    */
  override def configure(jafpl: Jafpl): Unit = {
    jafpl.traceEventManager = new DefaultTraceEventManager()
    jafpl.errorListener = new DefaultErrorListener()
  }
}
