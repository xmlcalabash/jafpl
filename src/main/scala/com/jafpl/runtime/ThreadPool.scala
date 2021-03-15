package com.jafpl.runtime

import com.jafpl.util.TraceEventManager

class ThreadPool(val scheduler: Scheduler, val size: Int) {
  private val actualSize = if (size < 1) {
    1
  } else {
    size
  }

  scheduler.runtime.traceEventManager.trace(s"CREAT thread pool with $size threads", TraceEventManager.THREADS)
  val threads = new Array[Option[Thread]](actualSize)
  for (pos <- 0 until actualSize) {
    threads(pos) = None
  }

  def getThread(action: Runnable): Option[Thread] = {
    synchronized {
      for (pos <- 0 until actualSize) {
        if (threads(pos).isEmpty || !threads(pos).get.isAlive) {
          val thread = new Thread(action)
          threads(pos) = Some(thread)
          scheduler.runtime.traceEventManager.trace(s"THRED ${thread.getId} at ${pos} for ${action}", TraceEventManager.THREADS)
          return threads(pos)
        }
      }

      return None
    }
  }

  def threadCount(): Int = {
    var count = 0
    for (pos <- 0 until actualSize) {
      if (threads(pos).isEmpty || !threads(pos).get.isAlive) {
        count += 1
      }
    }
    count
  }

  def idle: Boolean = {
    actualSize == threadCount()
  }

  def joinAll(): Unit = {
    scheduler.runtime.traceEventManager.trace("POOL  WAIT: " + threadCount(), TraceEventManager.THREADS)
    for (pos <- 0 until actualSize) {
      if (threads(pos).nonEmpty && threads(pos).get.isAlive) {
        scheduler.runtime.traceEventManager.trace("POOL  WAIT FOR " + pos, TraceEventManager.THREADS)
        threads(pos).get.join();
      }
    }
  }
}
