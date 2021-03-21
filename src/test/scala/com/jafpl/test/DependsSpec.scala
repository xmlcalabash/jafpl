package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Identity, Manifold, Producer, Sink, Sleep}
import org.scalatest.flatspec.AnyFlatSpec

class DependsSpec extends AnyFlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "Two independent steps " should " be unordered" in {
    val graph    = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val prodA = pipeline.addAtomic(new Producer(List("A")), "docA")
    graph.addEdge(prodA, "result", pipeline, "result")
    val prodB = pipeline.addAtomic(new Producer(List("B")), "docB")
    graph.addEdge(prodB, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

    assert(bc.items.size == 2)
    assert(bc.items.head == "A" || bc.items.head == "B")
    if (bc.items.head == "A") {
      assert(bc.items(1) == "B")
    } else {
      assert(bc.items(1) == "A")
    }
  }

  "If A depends on B, B " should " always be first" in {
    val graph    = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val prodA = pipeline.addAtomic(new Producer(List("A")), "docA")
    graph.addEdge(prodA, "result", pipeline, "result")
    val prodB = pipeline.addAtomic(new Producer(List("B")), "docB")
    graph.addEdge(prodB, "result", pipeline, "result")

    prodA.dependsOn(prodB)

    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

    assert(bc.items.size == 2)
    assert(bc.items.head == "B")
    assert(bc.items(1) == "A")
  }

  "If B depends on A, A " should " always be first" in {
    val graph    = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val prodA = pipeline.addAtomic(new Producer(List("A")), "docA")
    graph.addEdge(prodA, "result", pipeline, "result")
    val prodB = pipeline.addAtomic(new Producer(List("B")), "docB")
    graph.addEdge(prodB, "result", pipeline, "result")

    prodB.dependsOn(prodA)

    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

    assert(bc.items.size == 2)
    assert(bc.items.head == "A")
    assert(bc.items(1) == "B")
  }
}