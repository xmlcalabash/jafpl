package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.graph.Graph
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Producer, StringComposer, Uppercase}
import org.scalatest.FlatSpec

class ViewportSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "A viewport " should " do what a viewport does" in {
    val graph    = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline()

    val prod     = pipeline.addAtomic(new Producer(List("Now is the time; just do it.")), "prod")
    val viewport = pipeline.addViewport(new StringComposer(), "viewport")
    val uc       = viewport.addAtomic(new Uppercase(), "uc")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(prod, "result", viewport, "source")
    graph.addEdge(viewport, "current", uc, "source")
    graph.addEdge(uc, "result", viewport, "result")
    graph.addEdge(viewport, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    graph.close()
    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "NOW IS THE TIME; JUST DO IT.")
  }

  "A viewport " should " be able to have any output port name" in {
    val graph    = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline()

    val prod     = pipeline.addAtomic(new Producer(List("Now is the time; just do it.")), "prod")
    val viewport = pipeline.addViewport(new StringComposer(), "viewport")
    val uc       = viewport.addAtomic(new Uppercase(), "uc")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(prod, "result", viewport, "source")
    graph.addEdge(viewport, "current", uc, "source")
    graph.addEdge(uc, "result", viewport, "fribble")
    graph.addEdge(viewport, "fribble", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    graph.close()
    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "NOW IS THE TIME; JUST DO IT.")
  }

  "A viewport with no input " should " produce no output" in {
    val graph    = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline()

    val prod     = pipeline.addAtomic(new Producer(List()), "prod")
    val viewport = pipeline.addViewport(new StringComposer(), "viewport")
    val uc       = viewport.addAtomic(new Uppercase(), "uc")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(prod, "result", viewport, "source")
    graph.addEdge(viewport, "current", uc, "source")
    graph.addEdge(uc, "result", viewport, "result")
    graph.addEdge(viewport, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    graph.close()
    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.isEmpty)
  }
}