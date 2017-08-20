package com.jafpl.io

import com.jafpl.exceptions.GraphException
import com.jafpl.steps.{Consumer, PortBindingSpecification, Step}

class DevNullConsumer() extends Step {
  override def inputSpec = PortBindingSpecification.ANY
  override def outputSpec = PortBindingSpecification.ANY
  override def requiredBindings: Set[String] = Set()

  override def setConsumer(consumer: Consumer): Unit = {
    // nop
  }

  override def receiveBinding(variable: String, value: Any): Unit = {
    // nop
  }

  override def receive(port: String, item: Any): Unit = {
    if (port != "source") {
      throw new GraphException("Consumers must have a single input port named 'source'")
    }
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
