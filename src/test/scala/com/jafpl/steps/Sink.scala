package com.jafpl.steps

class Sink extends DefaultStep {
  override def inputSpec = PortBindingSpecification.SOURCE
  override def outputSpec = PortBindingSpecification.NONE
}
