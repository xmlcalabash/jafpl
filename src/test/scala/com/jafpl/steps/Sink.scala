package com.jafpl.steps

class Sink extends DefaultStep {
  override def inputSpec = PortSpecification.SOURCESEQ
  override def outputSpec = PortSpecification.NONE
}
