package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.graph.Graph
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Identity, Producer}
import org.scalatest.FlatSpec

class IOPipelineSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "A pipeline with inputs and outputs " should " run" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline()
    val producer = pipeline.addAtomic(new Producer(List("DOCUMENT")), "producer")
    val ident = pipeline.addAtomic(new Identity(), "ident1")
    val bc = new BufferSink()
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(producer, "result", pipeline, "source")
    graph.addEdge(pipeline, "source", ident, "source")
    graph.addEdge(ident, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "DOCUMENT")
  }
}