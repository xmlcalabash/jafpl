package com.jafpl.config

import com.jafpl.util.{DefaultErrorListener, DefaultTraceEventManager}

class DefaultJafplConfigurer extends JafplConfigurer {
  override def configure(jafpl: Jafpl): Unit = {
    jafpl.traceEventManager = new DefaultTraceEventManager()
    jafpl.errorListener = new DefaultErrorListener()
  }
}
