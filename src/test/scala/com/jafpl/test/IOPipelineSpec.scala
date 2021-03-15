package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.messages.{ItemMessage, Metadata}
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Identity, Manifold, Producer}
import org.scalatest.flatspec.AnyFlatSpec

class IOPipelineSpec extends AnyFlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "A pipeline with inputs and outputs " should " run" in {
    val graph    = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val ident = pipeline.addAtomic(new Identity(), "ident1")

    graph.addEdge(pipeline, "source", ident, "source")
    graph.addEdge(ident, "result", pipeline, "result")
    graph.addInput(pipeline, "source")
    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.inputs("source").send(new ItemMessage("P1", Metadata.BLANK))
    runtime.inputs("source").send(new ItemMessage("P2", Metadata.BLANK))
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

    assert(bc.items.size == 2)
    assert(bc.items(0) == "P1")
    assert(bc.items(1) == "P2")
  }
}