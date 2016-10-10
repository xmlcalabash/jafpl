package com.jafpl.graph

import com.jafpl.items.GenericItem
import com.jafpl.messages.ItemMessage
import com.jafpl.util.TreeWriter

/**
  * Created by ndw on 10/2/16.
  */
class OutputNode(graph: Graph, name: Option[String]) extends Node(graph, name, None) {
  private val items = collection.mutable.ListBuffer.empty[GenericItem]
  private var done = false
  private var constructionOk = true

  override def addOutput(port: String, edge: Option[Edge]): Unit = {
    constructionOk = false
    throw new GraphException("Cannot connect outputs to an OutputNode")
  }

  override def valid: Boolean = {
    super.valid && constructionOk
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    items.synchronized {
      items.append(msg.item)
    }
  }

  override def run(): Unit = {
    done = true
  }

  def closed = done

  def read(): Option[GenericItem] = {
    items.synchronized {
      if (items.isEmpty) {
        None
      } else {
        Some(items.remove(0))
      }
    }
  }

  override def dumpExtraAttr(tree: TreeWriter): Unit = {
    tree.addAttribute(Serializer._boundary, "true")
  }

}
