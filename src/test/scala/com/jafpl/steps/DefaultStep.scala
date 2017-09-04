package com.jafpl.steps

import com.jafpl.graph.Location
import com.jafpl.messages.Metadata
import com.jafpl.runtime.RuntimeConfiguration

class DefaultStep  extends Step {
  protected var location = Option.empty[Location]

  override def inputSpec: PortSpecification = PortSpecification.ANY
  override def outputSpec: PortSpecification = PortSpecification.ANY
  override def bindingSpec: BindingSpecification = BindingSpecification.ANY

  override def receiveBinding(variable: String, value: Any): Unit = {
    // nop
  }

  protected var consumer: Option[DataConsumer] = None

  override def setConsumer(consumer: DataConsumer): Unit = {
    this.consumer = Some(consumer)
  }

  override def setLocation(location: Location): Unit = {
    this.location = Some(location)
  }

  override def receive(port: String, item: Any, metadata: Metadata): Unit = {
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
