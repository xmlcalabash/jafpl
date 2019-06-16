package com.jafpl.steps

class Empty() extends DefaultStep {

  override def inputSpec: PortSpecification = {
    PortSpecification.NONE
  }
  override def outputSpec: PortSpecification = {
    PortSpecification.RESULTSEQ
  }
}
