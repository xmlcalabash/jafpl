package com.jafpl.graph

import com.jafpl.steps.{Manifold, ManifoldSpecification, PortSpecification, ViewportComposer}

private[jafpl] class ViewportStart(override val graph: Graph,
                                   override protected val end: ContainerEnd,
                                   override val userLabel: Option[String],
                                   val composer: ViewportComposer,
                                   private val manspec: ManifoldSpecification)
  extends LoopStart(graph, end, userLabel) {
  private var _outputPort = ""
  manifold = manspec

  def outputPort: String = _outputPort

  override def inputsOk(): Boolean = {
    var hasSource = false

    if (inputs.nonEmpty) {
      var valid = true
      for (port <- inputs) {
        if (!validPortName(port, "source")) {
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

    // A viewport can have only two outputs: current and result
    var count = 0
    for (output <- outputs) {
      if (output != "current" && !output.startsWith("#depends_")) {
        count += 1
        _outputPort = output
      }
    }

    valid && (count == 1)
  }
}
