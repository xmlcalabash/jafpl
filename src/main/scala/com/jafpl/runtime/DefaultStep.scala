package com.jafpl.runtime

import com.jafpl.messages.ItemMessage
import com.jafpl.util.SourceLocation
import org.slf4j.LoggerFactory

/**
  * Created by ndw on 10/7/16.
  */
abstract class DefaultStep extends Step  {
  protected val logger = LoggerFactory.getLogger(this.getClass)
  protected var controller: StepController = _
  protected var inputPorts = List.empty[String]
  protected var outputPorts = List.empty[String]
  private var _label: String = "unknown"
  private var _location: Option[SourceLocation] = None

  override def label = _label
  def label_=(label: String): Unit = {
    _label = label
  }

  override def location = _location
  def location_=(sourceLocation: SourceLocation): Unit = {
    _location = Some(sourceLocation)
  }

  override def setup(ctrl: StepController,
                     inputs: List[String],
                     outputs: List[String]): Unit = {
    //logger.debug("{} setup", name)
    controller = ctrl
    inputPorts = inputs
    outputPorts = outputs
  }

  override def reset(): Unit = {
    //logger.debug("{} reset", this)
  }

  override def run(): Unit = {
    //logger.debug("{} run", this)
  }

  override def teardown() = {
    //logger.debug("{} teardown", this)
  }

  override def receive(port: String, msg: ItemMessage): Unit = {
    //logger.debug("{} receive on {}: {}", name, port, msg)
  }
}
