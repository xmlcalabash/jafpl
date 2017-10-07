package com.jafpl.injection

trait StepInjectable extends GraphInjectable {
  def beforeRun(): Unit
  def afterRun(): Unit
}
