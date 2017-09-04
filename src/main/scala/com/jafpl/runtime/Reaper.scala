package com.jafpl.runtime

import akka.actor.{Actor, ActorRef, Terminated}
import org.slf4j.{Logger, LoggerFactory}

import scala.collection.mutable.ArrayBuffer

object Reaper {
  case class WatchMe(ref: ActorRef)
}

class Reaper(runtime: RuntimeConfiguration) extends Actor {
  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  import Reaper._

  private val watched = ArrayBuffer.empty[ActorRef]

  final def receive = {
    case WatchMe(ref) =>
      if (runtime.traceEnabled("reaper")) {
        logger.debug("The reaper is watching you, " + ref)
      }
      context.watch(ref)
      watched += ref
    case Terminated(ref) =>
      if (runtime.traceEnabled("reaper")) {
        logger.debug("The reaper gets your soul, " + ref)
      }
      watched -= ref
      if (watched.isEmpty) {
        if (runtime.traceEnabled("reaper")) {
          logger.debug("The reaper has run out of souls")
        }
        context.system.terminate()
      }
  }
}
