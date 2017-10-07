package com.jafpl.primitive

import com.jafpl.injection.StepInjectable
import com.jafpl.messages.BindingMessage

class PrimitiveStepInjectable extends StepInjectable {
  override def receiveBinding(message: BindingMessage): Unit = {
    println("PrimitiveStepInjectable received binding for " + message.name)
  }

  override def beforeRun(): Unit = {
    println("PrimitiveStepInjectable before run")
  }

  override def afterRun(): Unit = {
    println("PrimitiveStepInjectable after run")
  }
}
