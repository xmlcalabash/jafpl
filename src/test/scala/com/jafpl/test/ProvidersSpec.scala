package com.jafpl.test

import com.jafpl.graph.Graph
import com.jafpl.io.BufferConsumer
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.Identity
import org.scalatest.FlatSpec

class ProvidersSpec extends FlatSpec {
  val PIPELINEDATA = "Document"
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "Pipeline providers " should " should provide input and consume output" in {
    val graph = new Graph()

    val pipeline = graph.addPipeline(None)
    val ident = pipeline.addAtomic(new Identity(), "ident")

    graph.addEdge(pipeline, "source", ident, "source")
    graph.addEdge(ident, "result", pipeline, "result")

    graph.addInput(pipeline, "source")
    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)

    runtime.inputs("source").send(PIPELINEDATA)

    val bc = new BufferConsumer()
    runtime.outputs("result").setProvider(bc)

    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == PIPELINEDATA)
  }
}