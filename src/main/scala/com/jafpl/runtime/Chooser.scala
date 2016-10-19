package com.jafpl.runtime

import com.jafpl.graph.{GraphException, Node}
import com.jafpl.items.GenericItem
import com.jafpl.messages.{CloseMessage, ItemMessage}

import scala.collection.mutable

/**
  * Created by ndw on 10/10/16.
  */
class Chooser extends DefaultCompoundStep {
  val cDoc = mutable.HashMap.empty[String, GenericItem]
  var whenCount = 0

  label = "_chooser"

  def pickOne(nodes: List[Node]): Unit = {
    var when: Option[Node] = None
    while (when.isEmpty && whenCount < nodes.size) {
      whenCount += 1
      val iport = "I_choose_" + whenCount
      if (!cDoc.contains(iport)) {
        throw new GraphException("Choose ran out of whens")
      }

      val item = Some(cDoc(iport))
      val node = nodes(whenCount - 1)
      if (node.worker.get.asInstanceOf[WhenStep].test(item.get)) {
        when = Some(node)
      }
    }

    if (when.isEmpty) {
      throw new GraphException("Choose didn't find any takers")
    }

    for (node <- nodes) {
      if (when.get == node) {
        // This will cause the when to run as its last input will have been closed
        controller.tell(node, new CloseMessage(null, "condition"))
      } else {
        controller.finish(node)
      }
    }
  }

  override def runAgain: Boolean = false

  override def receive(port: String, msg: ItemMessage): Unit = {
    //logger.debug("{} receive on {}: {}", name, port, msg)
    cDoc.put(port, msg.item)
  }
}
