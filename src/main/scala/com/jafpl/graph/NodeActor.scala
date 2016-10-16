package com.jafpl.graph

import akka.actor.Actor
import akka.event.Logging
import com.jafpl.graph.GraphMonitor.{GFinish, GFinished, GStart}
import com.jafpl.messages._

import scala.collection.mutable

/**
  * Created by ndw on 10/3/16.
  */
private[graph] class NodeActor(node: Node) extends Actor {
  val log = Logging(context.system, this)
  val openInputs = mutable.HashSet() ++ node.inputs()
  val dependsOn = mutable.HashSet() ++ node.dependsOn

  log.debug("INIT " + node + ": " + openList)

  private def openList: String = {
    var str = ""
    for (input <- openInputs) {
      str += input + " "
    }
    str
  }

  def checkRun(): Unit = {
    if (openInputs.isEmpty && dependsOn.isEmpty) {
      run()
    } else {
      var str = ""
      for (port <- openInputs) {
        str += port + " "
      }
      log.debug("NOT READY " + node + ": " + str)
    }
  }

  private def run() = {
    node.graph.monitor ! GStart(node)
    node.synchronized {
      node.run()
    }
  }

  def receive = {
    case m: ItemMessage =>
      log.debug("A IMSSG {} {}", m.port, node)
      node.synchronized {
        node.receive(m.port, m)
      }
    case m: CloseMessage =>
      if (openInputs.contains(m.port)) {
        openInputs.remove(m.port)
        log.debug("A CLOSE OPEN {}: {}: {}", m.port, node, openList)
      } else {
        log.debug("A CLOSE FAIL {}: {}: {}", m.port, node, openList)
      }
      checkRun()
    case m: StartMessage =>
      log.debug("A START  {} CHECK", node)
      checkRun()
    case m: RanMessage =>
      log.debug("A RAN    {} CHECK", node)
      if (dependsOn.contains(m.node)) {
        dependsOn.remove(m.node)
        checkRun()
      }
    case m: ResetMessage =>
      log.debug("A RESET {}", node)
      node.synchronized {
        node.reset()
      }
    case m: GFinished =>
      log.debug("A FINIT {}", node)
      node match {
        case compoundStart: CompoundStart =>
          if (compoundStart.runAgain) {
            for (node <- compoundStart.subpipeline) {
              node.synchronized {
                node.reset()
              }
            }
          } else {
            node.graph.monitor ! GFinish(compoundStart)
            compoundStart.compoundEnd.stop()
          }
        case compoundEnd: CompoundEnd =>
          if (!compoundEnd.compoundStart.asInstanceOf[CompoundStart].runAgain) {
            node.graph.monitor ! GFinish(compoundEnd)
          }
        case _ => log.debug("Node {} didn't expect to be notified of subgraph completion", node)
      }
    case m: Any => log.debug("Node {} received unexpected message: {}", node, m)
  }
}
