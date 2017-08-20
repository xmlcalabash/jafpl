package com.jafpl.steps

class Sink extends DefaultStep {
  override def inputSpec = PortBindingSpecification.SOURCESEQ
  override def outputSpec = PortBindingSpecification.NONE
}
