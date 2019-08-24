package com.jafpl.steps

import com.jafpl.graph.Location
import com.jafpl.messages.{BindingMessage, Message}
import com.jafpl.runtime.RuntimeConfiguration

class DefaultStep  extends Step {
  protected var _location = Option.empty[Location]

  override def inputSpec: PortSpecification = PortSpecification.ANY
  override def outputSpec: PortSpecification = PortSpecification.ANY
  override def bindingSpec: BindingSpecification = BindingSpecification.ANY

  override def receiveBinding(message: BindingMessage): Unit = {
    // nop
  }

  protected var consumer: Option[DataConsumer] = None

  override def setConsumer(consumer: DataConsumer): Unit = {
    this.consumer = Some(consumer)
  }

  override def location: Option[Location] = _location
  override def location_=(location: Location): Unit = {
    _location = Some(location)
  }

  override def consume(port: String, message: Message): Unit = {
    // nop
  }

  override def initialize(config: RuntimeConfiguration): Unit = {
    // nop
  }

  override def run(): Unit = {
    // nop
  }

  override def reset(): Unit = {
    // nop
  }

  override def abort(): Unit = {
    // nop
  }

  override def stop(): Unit = {
    // nop
  }
}
