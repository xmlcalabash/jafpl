package com.jafpl.runtime
import com.jafpl.items.GenericItem
import com.jafpl.messages.ItemMessage
import net.sf.saxon.s9api.QName
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * Created by ndw on 10/10/16.
  */
class Whener(when: WhenStep) extends Step with CompoundStep with WhenStep {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected val cache = mutable.HashMap.empty[String,ListBuffer[ItemMessage]]
  private var ctrl: StepController = _
  private var _label = "_whener"

  override def label = _label
  override def label_=(label: String): Unit = {
    _label = label
  }

  override def setup(controller: StepController, inputPorts: List[String], outputPorts: List[String]): Unit = {
    when.setup(controller, inputPorts, outputPorts)
    ctrl = controller
  }

  override def reset(): Unit = {
    when.reset()
  }

  override def run(): Unit = {
    for (port <- cache.keySet) {
      val oport = "O_" + port.substring(2)
      for (msg <- cache(port)) {
        ctrl.send(oport, msg.item)
      }
      ctrl.close(oport)
    }
    cache.clear()
    when.run()
  }

  override def teardown(): Unit = {
    when.teardown()
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    if (port != "condition") {
      if (cache.contains(port)) {
        cache(port) += msg
      } else {
        cache.put(port, ListBuffer(msg))
      }
    }
  }

  override def receiveOutput(port: String, msg: ItemMessage): Unit = {
    when.receiveOutput(port, msg)
  }

  override def runAgain = false

  override def test(msg: GenericItem): Boolean = {
    when.test(msg)
  }
}
