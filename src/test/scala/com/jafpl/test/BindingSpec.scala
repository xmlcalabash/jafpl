package com.jafpl.test

import com.jafpl.graph.Graph
import com.jafpl.io.BufferConsumer
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.ProduceBinding
import org.scalatest.FlatSpec

class BindingSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "A binding " should " be available" in {
    var bc = new BufferConsumer()

    val graph = new Graph()
    val pipeline = graph.addPipeline()

    val binding  = pipeline.addBinding("x", "twelve")
    val binding2 = pipeline.addBinding("y", "eleven")
    val pb       = pipeline.addAtomic(new ProduceBinding("x"), "pb")
    val output   = graph.addAtomic(bc, "output")

    graph.addBindingEdge(binding, pb)
    graph.addBindingEdge(binding2, pb)
    graph.addEdge(pb, "result", pipeline.end, "result")
    graph.addEdge(pipeline, "result", output, "source")

    graph.close()
    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head== "twelve")
  }
}