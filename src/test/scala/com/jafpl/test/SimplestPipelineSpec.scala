package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Identity, Manifold, Producer, Sink, Sleep}
import org.scalatest.FlatSpec

class SimplestPipelineSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "The almost simplest possible pipeline " should " run" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val producer = pipeline.addAtomic(new Producer(List("DOCUMENT")), "doc")
    val sink = pipeline.addAtomic(new Sink(), "sink")

    graph.addEdge(producer, "result", sink, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()
  }

  "A pipeline with splits and joins " should " run" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val producer = pipeline.addAtomic(new Producer(List("DOCUMENT")), "producer")
    val ident1 = pipeline.addAtomic(new Identity(), "ident1")
    val ident2 = pipeline.addAtomic(new Identity(), "ident2")
    val sink = pipeline.addAtomic(new Sink(), "sink")

    graph.addEdge(producer, "result", ident1, "source")
    graph.addEdge(producer, "result", ident2, "source")

    graph.addEdge(ident1, "result", ident2, "source")
    graph.addEdge(ident2, "result", sink, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()
  }

  "A dependency " should " determine step order" in {
    val graph    = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1       = pipeline.addAtomic(new Producer(List("P1")), "P1")
    val p2       = pipeline.addAtomic(new Producer(List("P2")), "P2")
    val sleep    = pipeline.addAtomic(new Sleep(500), "sleep")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", pipeline, "result")
    graph.addEdge(p2, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")
    p2.dependsOn(sleep)

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 2)
    assert(bc.items(0) == "P1")
    assert(bc.items(1) == "P2")
  }

  "Multiple dependencies to the same step " should " be allowed" in {
    val graph    = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1       = pipeline.addAtomic(new Producer(List("P1")), "P1")
    val p2       = pipeline.addAtomic(new Producer(List("P2")), "P2")
    val p3       = pipeline.addAtomic(new Producer(List("P3")), "P3")
    val sleep    = pipeline.addAtomic(new Sleep(500), "sleep")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", pipeline, "result")
    graph.addEdge(p2, "result", pipeline, "result")
    graph.addEdge(p3, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")
    p2.dependsOn(sleep)
    p3.dependsOn(sleep)

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 3)
    assert(bc.items.head == "P1")
    // P2 and P3 can occur in either order
  }
}