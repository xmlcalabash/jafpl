package com.jafpl.steps

import com.jafpl.exceptions.PipelineException

class RaiseError(err: String) extends DefaultStep {
  override def inputSpec = PortSpecification.SOURCESEQ
  override def outputSpec = PortSpecification.RESULTSEQ

  override def run(): Unit = {
    throw new PipelineException(err, "Something bad happened: " + err)
  }
}
