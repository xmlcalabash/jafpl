package com.jafpl.test

import com.jafpl.graph.Graph
import com.jafpl.io.BufferConsumer
import com.jafpl.messages.Metadata
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Count, Identity, ProduceBinding}
import org.scalatest.FlatSpec

class BindingSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "An external document binding " should " be consumable" in {
    val graph = new Graph()

    val pipeline = graph.addPipeline()
    val ident    = pipeline.addAtomic(new Identity(), "ident")

    graph.addInput(pipeline, "source")
    graph.addEdge(pipeline, "source", ident, "source")
    graph.addEdge(ident, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    graph.close()
    val runtime = new GraphRuntime(graph, runtimeConfig)

    runtime.inputs("source").send("Hello, World", Metadata.STRING)

    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)

    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "Hello, World")
  }

  "An unbound external document binding " should " provides no documents" in {
    val graph = new Graph()

    val pipeline = graph.addPipeline()
    val ident    = pipeline.addAtomic(new Identity(), "ident")

    graph.addInput(pipeline, "source")
    graph.addEdge(pipeline, "source", ident, "source")
    graph.addEdge(ident, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    graph.close()
    val runtime = new GraphRuntime(graph, runtimeConfig)

    //runtime.inputs("source").receive("foo", "Hello, World", Metadata.STRING)

    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)

    runtime.run()

    assert(bc.items.isEmpty)
  }

  "A variable binding " should " be available" in {
    var bc = new BufferSink()

    val graph = new Graph()
    val pipeline = graph.addPipeline()

    val binding  = pipeline.addVariable("x", "twelve")
    val binding2 = pipeline.addVariable("y", "eleven")
    val pb       = pipeline.addAtomic(new ProduceBinding("x"), "pb")
    val output   = pipeline.addAtomic(bc, "output")

    graph.addBindingEdge(binding, pb)
    graph.addBindingEdge(binding2, pb)
    graph.addEdge(pb, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", output, "source")

    graph.close()
    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "twelve")
  }

  "An external variable binding " should " be consumable" in {
    val graph = new Graph()

    val binding  = graph.addBinding("foo")
    val pipeline = graph.addPipeline()

    val pb       = pipeline.addAtomic(new ProduceBinding("foo"), "pb")

    graph.addBindingEdge(binding, pb)
    graph.addEdge(pb, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    graph.close()
    val runtime = new GraphRuntime(graph, runtimeConfig)

    runtime.bindings("foo").set("Spoon!")

    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)

    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "Spoon!")
  }

  "Reading an external variable binding twice " should " work" in {
    val graph = new Graph()

    val binding  = graph.addBinding("foo")
    val pipeline = graph.addPipeline()

    val pb1      = pipeline.addAtomic(new ProduceBinding("foo"), "pb")
    val pb2      = pipeline.addAtomic(new ProduceBinding("foo"), "pb")
    val count    = pipeline.addAtomic(new Count(), "count")

    graph.addBindingEdge("foo", pb1)
    graph.addBindingEdge("foo", pb2)

    graph.addEdge(pb1, "result", count, "source")
    graph.addEdge(pb2, "result", count, "source")

    graph.addEdge(count, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)

    runtime.bindings("foo").set("Spoon!")

    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == 2)
  }

  "Leaving an external variable binding unbound " should " be an error" in {
    val graph = new Graph()

    val binding  = graph.addBinding("foo")
    val pipeline = graph.addPipeline()

    val pb       = pipeline.addAtomic(new ProduceBinding("foo"), "pb")

    graph.addBindingEdge(binding, pb)
    graph.addEdge(pb, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    graph.close()
    val runtime = new GraphRuntime(graph, runtimeConfig)

    //runtime.bindings("foo").set("Spoon!")

    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)

    var pass = false
    try {
      runtime.run()
    } catch {
      case _: Throwable => pass = true
    }

    assert(pass)
  }
}