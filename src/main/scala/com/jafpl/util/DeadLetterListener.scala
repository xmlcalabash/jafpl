package com.jafpl.util

import akka.actor.{Actor, DeadLetter}
import org.slf4j.{Logger, LoggerFactory}

class DeadLetterListener extends Actor {
  protected[jafpl] val logger: Logger = LoggerFactory.getLogger(this.getClass)

  def receive = {
    case d: DeadLetter =>
      logger.debug(d.toString)
    case _ => Unit
  }
}
