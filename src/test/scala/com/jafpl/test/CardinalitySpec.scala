package com.jafpl.test

import com.jafpl.graph.Graph
import com.jafpl.io.BufferConsumer
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{Identity, LiesAboutOutputBindings, Producer, Sink, Sleep}
import org.scalatest.FlatSpec

class CardinalitySpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "Incorrect input cardinalities " should " cause the pipeline to fail" in {
    val graph = new Graph()
    val bc = new BufferConsumer()

    val pipeline = graph.addPipeline()
    val p1       = pipeline.addAtomic(new Producer(List("P1", "P2")), "producer")
    val ident    = pipeline.addAtomic(new Identity(false), "identity")
    val consumer = graph.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", ident, "source")
    graph.addEdge(ident, "result", pipeline.end, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    var pass = false
    try {
      val runtime = new GraphRuntime(graph, runtimeConfig)
      runtime.run()
    } catch {
      case _: Throwable => pass = true
    }

    assert(pass)
  }

  "Incorrect output cardinalities " should " cause the pipeline to fail" in {
    val graph = new Graph()
    val bc = new BufferConsumer()

    val pipeline = graph.addPipeline()
    val p1       = pipeline.addAtomic(new Producer(List("P1", "P2")), "producer")
    val liar     = pipeline.addAtomic(new LiesAboutOutputBindings(), "liar")
    val consumer = graph.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", liar, "source")
    graph.addEdge(liar, "result", pipeline.end, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    var pass = false
    try {
      val runtime = new GraphRuntime(graph, runtimeConfig)
      runtime.run()
    } catch {
      case _: Throwable => pass = true
    }

    assert(pass)
  }
}