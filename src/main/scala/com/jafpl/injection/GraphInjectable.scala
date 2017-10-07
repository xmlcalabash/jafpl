package com.jafpl.injection

import com.jafpl.messages.BindingMessage

trait GraphInjectable {
  def receiveBinding(message: BindingMessage)
}
