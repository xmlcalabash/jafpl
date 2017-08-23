package com.jafpl.graph

import com.jafpl.steps.ViewportComposer

private[jafpl] class ViewportStart(override val graph: Graph,
                                   override protected val end: ContainerEnd,
                                   override val userLabel: Option[String],
                                   val composer: ViewportComposer)
  extends LoopStart(graph, end, userLabel) {

  override def inputsOk(): Boolean = {
    var hasSource = false

    if (inputs.nonEmpty) {
      var valid = true
      for (port <- inputs) {
        if (port != "#bindings" && port != "source") {
          println("Invalid binding on " + this + ": " + port)
          valid = false
        }
        hasSource = hasSource || (port == "source")
      }
      valid && hasSource
    } else {
      true
    }
  }

  override def outputsOk(): Boolean = {
    var valid = true

    var count = 0
    for (output <- outputs) {
      if (!inputs.contains(output)) {
        count += 1
        valid = valid && (count == 1) && (output == "result")
      }
    }

    valid
  }
}
