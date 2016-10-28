package com.jafpl.graph

import com.jafpl.graph.GraphMonitor.GSend
import com.jafpl.items.GenericItem
import com.jafpl.messages.ItemMessage
import com.jafpl.util.XmlWriter

/**
  * Created by ndw on 10/2/16.
  */
class InputNode(graph: Graph, val port: String) extends Node(graph, None) {
  private var constructionOk = true
  private var seqNo: Long = 1
  label = Some("_input_" + port)

  private[graph] override def addInput(port: String, edge: Option[Edge]): Unit = {
    constructionOk = false
    throw new GraphException("Cannot connect inputs to an InputNode")
  }

  private[graph] override def valid: Boolean = {
    super.valid && constructionOk
  }

  override private[graph] def run(): Unit = {
    close()
  }

  def write(item: GenericItem): Unit = {
    for (port <- outputs) {
      val edge = output(port)
      val targetPort = edge.get.inputPort
      val targetNode = edge.get.destination

      val msg = new ItemMessage(targetPort, uid, seqNo, item)
      seqNo += 1

      graph.monitor ! GSend(targetNode, msg)
    }
  }

  def close(): Unit = {
    stop()
  }

  override def dumpExtraAttr(tree: XmlWriter): Unit = {
    tree.addAttribute(Serializer._boundary, "true")
  }
}
