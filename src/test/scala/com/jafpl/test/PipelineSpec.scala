package com.jafpl.test

import com.jafpl.drivers.GraphTest.runtimeConfig
import com.jafpl.graph.Graph
import com.jafpl.io.BufferConsumer
import com.jafpl.messages.Metadata
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Identity, Producer, Sink, Sleep}
import org.scalatest.FlatSpec

class PipelineSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "Pipelines " should " allow multiple inputs" in {
    val graph = new Graph()

    val pipeline  = graph.addPipeline()
    val ident     = pipeline.addAtomic(new Identity(), "ident")

    graph.addEdge(pipeline, "source1", ident, "source")
    graph.addEdge(pipeline, "source2", ident, "source")
    graph.addEdge(ident, "result", pipeline, "result")

    graph.addInput(pipeline, "source1")
    graph.addInput(pipeline, "source2")
    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.inputs("source1").receive("source1", "Hello", Metadata.BLANK)
    runtime.inputs("source2").receive("source", "World", Metadata.BLANK)

    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)

    runtime.run()

    assert(bc.items.size == 2)
    assert(((bc.items(0) == "Hello") && (bc.items(1) == "World"))
      || ((bc.items(0) == "World") && (bc.items(1) == "Hello")))

  }

  "Pipelines " should " allow multiple outputs" in {
    val graph = new Graph()

    val pipeline  = graph.addPipeline()
    val producer1 = pipeline.addAtomic(new Producer("ONE"), "producer1")
    val producer2 = pipeline.addAtomic(new Producer("TWO"), "producer2")

    graph.addOutput(pipeline, "result1")
    graph.addOutput(pipeline, "result2")

    graph.addEdge(producer1, "result", pipeline, "result1")
    graph.addEdge(producer2, "result", pipeline, "result2")

    val runtime = new GraphRuntime(graph, runtimeConfig)

    val bc1 = new BufferConsumer()
    runtime.outputs("result1").setConsumer(bc1)
    val bc2 = new BufferConsumer()
    runtime.outputs("result2").setConsumer(bc2)

    runtime.run()

    assert(bc1.items.size == 1)
    assert(bc1.items.head == "ONE")

    assert(bc2.items.size == 1)
    assert(bc2.items.head == "TWO")
  }
}