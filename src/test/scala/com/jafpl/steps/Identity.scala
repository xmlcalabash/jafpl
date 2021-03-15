package com.jafpl.steps

import com.jafpl.messages.Message

import scala.collection.mutable.ListBuffer

class Identity(allowSeq: Boolean, label: String) extends DefaultStep {
  var ready = false
  val buf = ListBuffer.empty[Message]

  def this() = {
    this(true, "")
  }

  def this(allowSeq: Boolean) = {
    this(allowSeq, "")
  }

  override def inputSpec: PortSpecification = {
    if (allowSeq) {
      PortSpecification.SOURCESEQ
    } else {
      PortSpecification.SOURCE
    }
  }
  override def outputSpec: PortSpecification = {
    if (allowSeq) {
      PortSpecification.RESULTSEQ
    } else {
      PortSpecification.RESULT
    }
  }

  override def run(): Unit = {
    //println(s"ID $label RUN     | $this")
    super.run()
    ready = true
    for (item <- buf) {
      //println(s"ID $label SEND: $item | $this")
      consumer.get.consume("result", item)
    }
    buf.clear()
  }

  override def reset(): Unit = {
    //println(s"ID $label RESET   | $this")
    super.reset()
    buf.clear()
    ready = false
  }

  override def consume(port: String, message: Message): Unit = {
    //println(s"ID $label RECV: $message | $this")
    if (ready) {
      //println(s"ID $label SEND: $message | $this")
      consumer.get.consume("result", message)
    } else {
      buf += message
    }
  }
}
