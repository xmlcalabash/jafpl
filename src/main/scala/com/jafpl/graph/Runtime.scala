package com.jafpl.graph

import com.jafpl.graph.GraphMonitor.GWatchdog
import com.jafpl.items.GenericItem
import com.jafpl.runtime.{GraphRuntime, Step}
import org.slf4j.LoggerFactory

/**
  * Created by ndw on 10/6/16.
  */
private[graph] class Runtime(val graph: Graph) extends GraphRuntime {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private var _started = false
  private val watchdog = 10
  private val sleepInterval = 100

  graph.makeActors()

  override def run(): Unit = {
    if (!_started) {
      graph.runActors()
      _started = true
    }
  }

  override def running: Boolean = {
    if (_started) {
      if (graph.finished) {
        _started = false
      }
      !graph.finished
    } else {
      false
    }
  }

  def inputs(): List[InputNode] = {
    graph.inputs()
  }

  def outputs(): List[OutputNode] = {
    graph.outputs()
  }

  def read(port: String): Option[GenericItem] = {
    for (node <- outputs()) {
      if (node.port == port) {
        return node.read()
      }
    }
    logger.debug("Pipeline has no output port: " + port)
    None
  }

  def write(port: String, item: GenericItem): Unit = {
    if (!_started) {
      throw new GraphException("Cannot write to pipeline before it is started")
    }

    for (node <- inputs()) {
      if (node.port == port) {
        node.write(item)
        return
      }
    }

    logger.debug("Pipeline has no input port: " + port)
  }

  def close(port: String): Unit = {
    if (_started) {
      for (node <- inputs()) {
        if (node.port == port) {
          node.close()
          return
        }
      }

      logger.info("Pipeline has no input port: " + port)
    }
  }

  override def reset(): Unit = {
    graph.reset()
  }

  override def waitForPipeline: Option[Throwable] = {
    var ticker = watchdog * 1000
    while (!graph.finished) {
      if (ticker <= 0) {
        ticker = watchdog * 1000
        graph.monitor ! GWatchdog(ticker)
      }
      Thread.sleep(sleepInterval)
      ticker -= sleepInterval
    }
    if (graph.exception.isDefined) {
      throw graph.exception.get
    }
    graph.exception
  }

  override def teardown(): Unit = {
    graph.teardown()
  }

  override def exception: Option[Throwable] = graph.exception

  override def exceptionStep: Option[Step] = {
    if (graph.exceptionNode.isDefined) {
      graph.exceptionNode.get.step
    } else {
      None
    }
  }
}
