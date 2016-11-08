package com.jafpl.graph

import com.jafpl.graph.GraphMonitor.GSend
import com.jafpl.items.GenericItem
import com.jafpl.messages.ItemMessage

/**
  * Created by ndw on 10/2/16.
  */
private[graph] class InputOption(graph: Graph) extends Node(graph, None) {
  private var constructionOk = true
  private var seqNo: Long = 1
  private var initialized = false
  label = Some("_input_option")

  private[graph] override def addInput(port: String, edge: Option[Edge]): Unit = {
    constructionOk = false
    throw new GraphException("Cannot connect inputs to an InputOption")
  }

  private[graph] override def valid: Boolean = {
    super.valid && constructionOk
  }

  override private[graph] def run(): Unit = {
    // do nothing
  }

  def set(item: GenericItem): Unit = {
    if (initialized) {
      throw new GraphException("You cannot reinitialize an option")
    }

    for (port <- outputs) {
      val edge = output(port)
      val targetPort = edge.get.inputPort
      val targetNode = edge.get.destination

      val msg = new ItemMessage(targetPort, uid, seqNo, item)
      seqNo += 1

      graph.monitor ! GSend(targetNode, msg)
    }

    initialized = true
  }
}
