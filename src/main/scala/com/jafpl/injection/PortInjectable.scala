package com.jafpl.injection

import com.jafpl.messages.ItemMessage

trait PortInjectable extends GraphInjectable {
  def port: String
  def run(context: ItemMessage): Unit
}
