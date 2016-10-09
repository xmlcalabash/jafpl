package com.jafpl.runtime

import com.jafpl.graph.Node
import com.jafpl.messages.ItemMessage
import net.sf.saxon.s9api.QName
import org.slf4j.LoggerFactory

/**
  * Created by ndw on 10/7/16.
  */
abstract class DefaultStart(name: String, val subpipeline: List[Node]) extends DefaultStep(name) with CompoundStart {
  private val _compoundEnd = new DefaultEnd(this)
  private var _ready = true
  private var _needsRestart = false
  protected val semaphore = "semaphore"

  def compoundEnd = _compoundEnd

  def ready = _ready
  def ready_=(value: Boolean) = {
    _ready = value
  }

  def needsRestart = _needsRestart
  def needsRestart_=(value: Boolean) = {
    _needsRestart = value
  }

  override def setup(ctrl: StepController,
                     inputs: List[String],
                     outputs: List[String],
                     opts: List[QName]): Unit = {
    super.setup(ctrl, inputs, outputs, opts)
  }

  override def readyToRestart(): Unit = {
    ready = true
    needsRestart = true
  }

  override def receiveResult(port: String, msg: ItemMessage): Unit = {
    logger.info("DOCUMENT RECEIVED FROM END")
    logger.debug("{} receiveResult on {}: {}", this, port, msg)
    _compoundEnd.receiveResult(port, msg)
  }
}
