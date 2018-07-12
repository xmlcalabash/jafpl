package com.jafpl.steps

class RaiseError(err: String) extends DefaultStep {
  override def inputSpec = PortSpecification.SOURCESEQ
  override def outputSpec = PortSpecification.RESULTSEQ

  override def run(): Unit = {
    throw new RaiseErrorException(err, "Something bad happened: " + err, location)
  }
}
