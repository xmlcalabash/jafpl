package com.jafpl.config

trait JafplConfigurer {
  def configure(jafpl: Jafpl): Unit
}
