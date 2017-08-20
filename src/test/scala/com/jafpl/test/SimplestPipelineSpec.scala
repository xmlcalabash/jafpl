package com.jafpl.test

import com.jafpl.graph.Graph
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{Identity, Producer, Sink}
import org.scalatest.FlatSpec

class SimplestPipelineSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "The almost simplest possible pipeline " should " run" in {
    val graph = new Graph()
    val pipeline = graph.addPipeline()
    val producer = pipeline.addAtomic(new Producer(List("DOCUMENT")), "doc")
    val sink = pipeline.addAtomic(new Sink(), "sink")

    graph.addEdge(producer, "result", sink, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()
  }

  "A pipeline with splits and joins " should " run" in {
    val graph = new Graph()
    val pipeline = graph.addPipeline()
    val producer = graph.addAtomic(new Producer(List("DOCUMENT")), "producer")
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
}