package com.jafpl.calc

import com.jafpl.graph.Node
import com.jafpl.items.NumberItem
import com.jafpl.messages.{ItemMessage, ResetMessage}
import com.jafpl.runtime.{CompoundEnd, CompoundStart, DefaultStart, DefaultStep}

import scala.collection.mutable

/**
  * Created by ndw on 10/9/16.
  */
class GenerateIntegers(name: String, subpipeline: List[Node]) extends DefaultStart(name, subpipeline) {
  val integers = mutable.HashMap.empty[Int, Int]
  var index = 1

  override def run(): Unit = {
    semaphore.synchronized {
      logger.info("RUN GENINT")
      if (integers.contains(index)) {
        controller.send("current", new NumberItem(integers(index) * 2))
        controller.close("current")
        index += 1
      }
    }
  }

  override def readyToRestart(): Unit = {
    semaphore.synchronized {
      logger.info("READY TO RESTART ON GENINT: " + index + ": " + integers.size)
      super.readyToRestart()
      if (integers.contains(index)) {
        logger.info("SEND ON GENINT")
        restartPipeline()
        controller.send("current", new NumberItem(integers(index) * 2))
        controller.close("current")
        index += 1
      }
    }
  }

  private def restartPipeline(): Unit = {
    logger.info("RESTART GENINT")
    needsRestart = false
    for (node <- subpipeline) {
      controller.tell(node, new ResetMessage())
    }
  }

  override def finished: Boolean = {
    semaphore.synchronized {
      logger.info("FINISHED GENINT " + index + ": " + integers.size)
      index > integers.size
    }
  }

  override def completed: Boolean = {
    semaphore.synchronized {
      logger.info("COMPLETED GENINT " + index + ": " + integers.size)
      index > integers.size
    }
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    semaphore.synchronized {
      val index = port.substring(1).toInt

      msg.item match {
        case num: NumberItem =>
          logger.info("RECEIVED GENINT " + index)
          integers.put(index, num.get)
        case _ => throw new CalcException("Message was not a number")
      }
    }
  }
}
