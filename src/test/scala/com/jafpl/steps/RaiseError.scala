package com.jafpl.steps

import com.jafpl.exceptions.PipelineException

class RaiseError(err: String) extends DefaultStep {
  override def inputSpec = PortBindingSpecification.SOURCESEQ
  override def outputSpec = PortBindingSpecification.RESULTSEQ

  override def run(): Unit = {
    throw new PipelineException(err, "Something bad happened: " + err)
  }
}
