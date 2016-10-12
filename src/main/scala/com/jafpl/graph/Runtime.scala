package com.jafpl.graph

import com.jafpl.items.GenericItem
import net.sf.saxon.s9api.QName
import org.slf4j.LoggerFactory

/**
  * Created by ndw on 10/6/16.
  */
class Runtime(val graph: Graph) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private var started = false

  if (!graph.valid) {
    throw new GraphException("Invalid graph")
  }

  def inputs(): List[InputNode] = {
    if (!started) {
      throw new GraphException("You must start the pipeline first!")
    }

    graph.inputs()
  }

  def options(): List[InputOption] = {
    if (!started) {
      throw new GraphException("You must start the pipeline first!")
    }

    graph.options()
  }

  def outputs(): List[OutputNode] = {
    if (!started) {
      throw new GraphException("You must start the pipeline first!")
    }

    graph.outputs()
  }

  def read(port: String): Option[GenericItem] = {
    for (node <- outputs()) {
      if (node.port == port) {
        return node.read()
      }
    }

    logger.info("Pipeline has no output port: " + port)
    None
  }

  def write(port: String, item: GenericItem): Unit = {
    if (!started) {
      throw new GraphException("You must start the pipeline first!")
    }

    for (node <- inputs()) {
      if (node.port == port) {
        node.write(item)
        return
      }
    }

    logger.info("Pipeline has no input port: " + port)
  }

  def close(port: String): Unit = {
    if (!started) {
      throw new GraphException("You must start the pipeline first!")
    }

    for (node <- inputs()) {
      if (node.port == port) {
        node.close()
        return
      }
    }

    logger.info("Pipeline has no input port: " + port)
  }

  def start(): Unit = {
    graph.makeActors()
    started = true
  }

  def running: Boolean = !graph.finished

  def kill(): Unit = {
    graph.system.terminate()
  }



}
