package com.jafpl.xpath

import net.sf.saxon.s9api.{Axis, XdmItem, XdmNode, XdmNodeKind}

import scala.collection.mutable.{ArrayBuffer, ListBuffer}

/**
  * Created by ndw on 10/9/16.
  */
object XdmNodes {
  def elementChildren(node: XdmNode): List[XdmNode] = {
    val children = ListBuffer.empty[XdmNode]
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val item = iter.next().asInstanceOf[XdmNode]
      if (item.getNodeKind == XdmNodeKind.ELEMENT) {
        children += item
      }
    }
    children.toList
  }

  def children(node: XdmNode): List[XdmNode] = {
    val children = ListBuffer.empty[XdmNode]
    val iter = node.axisIterator(Axis.CHILD)
    while (iter.hasNext) {
      val item = iter.next().asInstanceOf[XdmNode]
      if (item.getNodeKind == XdmNodeKind.ELEMENT
        || item.getNodeKind == XdmNodeKind.TEXT) {
        children += item
      }
    }
    children.toList
  }
}
