package com.jafpl.steps

trait DataConsumer extends Consumer {
  def setProvider(provider: DataProvider): Unit
}
