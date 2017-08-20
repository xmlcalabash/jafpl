package com.jafpl.steps

class DefaultStep  extends Step {
  override def inputSpec: PortBindingSpecification = PortBindingSpecification.ANY
  override def outputSpec: PortBindingSpecification = PortBindingSpecification.ANY
  override def requiredBindings: Set[String] = Set()

  override def receiveBinding(variable: String, value: Any): Unit = {
    // nop
  }

  protected var consumer: Option[Consumer] = None

  override def setConsumer(consumer: Consumer): Unit = {
    this.consumer = Some(consumer)
  }

  override def receive(port: String, item: Any): Unit = {
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
