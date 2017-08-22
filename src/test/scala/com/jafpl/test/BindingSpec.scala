package com.jafpl.test

import com.jafpl.graph.Graph
import com.jafpl.io.BufferConsumer
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, ProduceBinding}
import org.scalatest.FlatSpec

class BindingSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "A binding " should " be available" in {
    var bc = new BufferSink()

    val graph = new Graph()
    val pipeline = graph.addPipeline()

    val binding  = pipeline.addVariable("x", "twelve")
    val binding2 = pipeline.addVariable("y", "eleven")
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
    assert(bc.items.head == "twelve")
  }

  "An external binding " should " be consumable" in {
    val graph = new Graph()

    val binding  = graph.addBinding("foo")
    val pipeline = graph.addPipeline()

    val pb       = pipeline.addAtomic(new ProduceBinding("foo"), "pb")

    graph.addBindingEdge(binding, pb)
    graph.addEdge(pb, "result", pipeline.end, "result")
    graph.addOutput(pipeline, "result")

    graph.close()
    val runtime = new GraphRuntime(graph, runtimeConfig)

    runtime.bindings("foo").set("Spoon!")

    val bc = new BufferConsumer()
    runtime.outputs("result").setProvider(bc)

    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "Spoon!")
  }

  "Leaving an external binding unbound " should " be an error" in {
    val graph = new Graph()

    val binding  = graph.addBinding("foo")
    val pipeline = graph.addPipeline()

    val pb       = pipeline.addAtomic(new ProduceBinding("foo"), "pb")

    graph.addBindingEdge(binding, pb)
    graph.addEdge(pb, "result", pipeline.end, "result")
    graph.addOutput(pipeline, "result")

    graph.close()
    val runtime = new GraphRuntime(graph, runtimeConfig)

    //runtime.bindings("foo").set("Spoon!")

    val bc = new BufferConsumer()
    runtime.outputs("result").setProvider(bc)

    var pass = false
    try {
      runtime.run()
    } catch {
      case _: Throwable => pass = true
    }

    assert(pass)
  }
}