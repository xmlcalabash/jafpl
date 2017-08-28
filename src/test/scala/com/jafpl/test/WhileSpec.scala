package com.jafpl.test

import com.jafpl.graph.Graph
import com.jafpl.io.BufferConsumer
import com.jafpl.primitive.{PrimitiveItemTester, PrimitiveRuntimeConfiguration}
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{Decrement, Producer}
import org.scalatest.FlatSpec

class WhileSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "A while " should " iterate until finished" in {
    val graph = new Graph()
    val pipeline = graph.addPipeline()
    val p1       = pipeline.addAtomic(new Producer(List(7)), "p1")

    val tester   = new PrimitiveItemTester(runtimeConfig, ". > 0")
    val wstep    = pipeline.addWhile(tester)
    val decr     = wstep.addAtomic(new Decrement(), "decr")

    graph.addEdge(p1, "result", wstep, "source")
    graph.addEdge(wstep, "source", decr, "source")
    graph.addEdge(decr, "result", wstep, "result")

    graph.addEdge(wstep, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == 0)
  }

  "A while " should " not iterate at all if it's condition is initially false" in {
    val graph = new Graph()
    val pipeline = graph.addPipeline()
    val p1       = pipeline.addAtomic(new Producer(List(0)), "p1")

    val tester   = new PrimitiveItemTester(runtimeConfig, ". > 0")
    val wstep    = pipeline.addWhile(tester)
    val decr     = wstep.addAtomic(new Decrement(), "decr")

    graph.addEdge(p1, "result", wstep, "source")
    graph.addEdge(wstep, "source", decr, "source")
    graph.addEdge(decr, "result", wstep, "result")

    graph.addEdge(wstep, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == 0)
  }

}