package com.jafpl.graph

import akka.actor.Actor
import akka.event.Logging
import com.jafpl.graph.GraphMonitor.{GClose, GException, GFinish}
import com.jafpl.graph.NodeActor.{NException, NFinished, NReset, NRun}
import com.jafpl.messages.ItemMessage

/**
  * Created by ndw on 10/3/16.
  */

object NodeActor {
  case class NRun()
  case class NReset()
  case class NException(node: Node, srcNode: Node, throwable: Throwable)
  case class NFinished()
}

private[graph] class NodeActor(node: Node) extends Actor {
  val log = Logging(context.system, this)

  log.debug("INIT " + node)

  private def run() = {
    node match {
      case end: CompoundEnd => Unit
      case _ => node.run()
    }

    if (node.isInstanceOf[CompoundEnd]) {
      // nop
    } else {
      for (port <- node.outputs) {
        node.graph.monitor ! GClose(node, port)
      }
    }

    node.graph.monitor ! GFinish(node)
  }

  def receive = {
    case m: NRun =>
      //log.info("N RUN    {}", node)
      run()
    case m: NReset =>
      //log.info("N RESET  {}", node)
      node.reset()
    case m: NException =>
      if (!node.caught(m.throwable)) {
        node.graph.monitor ! GException(node, m.srcNode, m.throwable)
      }
    case m: ItemMessage =>
      //log.info("N RECEIVE {}: {}", m.port, m.item)
      node.receive(m.port, m)
    case m: NFinished =>
      //log.info("N FINISH {}", node)
      node.teardown()
    case m: Any => log.info("Node {} received unexpected message: {}", node, m)
  }
}
