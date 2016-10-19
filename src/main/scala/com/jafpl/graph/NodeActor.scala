package com.jafpl.graph

import akka.actor.Actor
import akka.event.Logging
import com.jafpl.graph.GraphMonitor.{GException, GFinish, GFinished, GStart, GWaitingFor}
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
  node.graph.monitor ! GWaitingFor(node, openInputs.toList, dependsOn.toList)

  private def openList: String = {
    var str = ""
    for (input <- openInputs) {
      str += input + " "
    }
    str
  }

  def checkRun(reason: String): Unit = {
    if (openInputs.isEmpty && dependsOn.isEmpty) {
      println("ARUN " + reason + ": " + this + ":" + node)
      openInputs ++ node.inputs()
      dependsOn ++ node.dependsOn
      node.graph.monitor ! GWaitingFor(node, openInputs.toList, dependsOn.toList)
      run()
    } else {
      println("AXXX " + reason + ": " + this + ":" + node)
      var str = ""
      for (port <- openInputs) {
        str += port + " "
      }
      log.debug("NOT READY " + node + ": " + str)
    }
  }

  private def run() = {
    node match {
      case end: CompoundEnd => Unit
      case _ =>
        node.graph.monitor ! GStart(node)
        node.synchronized {
          node.run()
        }
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
        node.graph.monitor ! GWaitingFor(node, openInputs.toList, dependsOn.toList)
      } else {
        log.debug("A CLOSE FAIL {}: {}: {}", m.port, node, openList)
      }
      checkRun("CLOSE " + m.node)
    case m: StartMessage =>
      log.debug("A START  {} CHECK", node)
      checkRun("START")
    case m: RanMessage =>
      log.debug("A RAN    {} CHECK", node)
      if (dependsOn.contains(m.node)) {
        dependsOn.remove(m.node)
        node.graph.monitor ! GWaitingFor(node, openInputs.toList, dependsOn.toList)
        checkRun("RAN")
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
                println("RESET NODE: " + node)
                node.reset()
              }
            }
            println("RESET START: " + compoundStart)
            compoundStart.reset()
            checkRun("LOOP")
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
    case m: GException =>
      if (!node.caught(m.throwable)) {
        node.graph.monitor ! GException(node, m.srcNode, m.throwable)
      }
    case m: Any => log.info("Node {} received unexpected message: {}", node, m)
  }
}
