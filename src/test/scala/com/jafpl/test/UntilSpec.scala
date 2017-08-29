package com.jafpl.test

import com.jafpl.graph.Graph
import com.jafpl.io.BufferConsumer
import com.jafpl.primitive.{PrimitiveItemComparator, PrimitiveRuntimeConfiguration}
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{Decrement, Producer}
import org.scalatest.FlatSpec

class UntilSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "An until " should " iterate until finished" in {
    val graph = new Graph()
    val pipeline = graph.addPipeline()
    val p1       = pipeline.addAtomic(new Producer(List(7)), "p1")

    val comp = new PrimitiveItemComparator()

    val ustep    = pipeline.addUntil(comp)
    val decr     = ustep.addAtomic(new Decrement(), "decr")

    graph.addEdge(p1, "result", ustep, "source")
    graph.addEdge(ustep, "current", decr, "source")
    graph.addEdge(decr, "result", ustep, "test")
    graph.addEdge(decr, "result", ustep, "result")

    graph.addEdge(ustep, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.run()

    assert(bc.items.size == 7)
  }

  "An until " should " require a test" in {
    val graph = new Graph()
    val pipeline = graph.addPipeline()
    val p1       = pipeline.addAtomic(new Producer(List(7)), "p1")

    val comp = new PrimitiveItemComparator()

    val ustep    = pipeline.addUntil(comp)
    val decr     = ustep.addAtomic(new Decrement(), "decr")

    graph.addEdge(p1, "result", ustep, "source")
    graph.addEdge(ustep, "current", decr, "source")
    graph.addEdge(decr, "result", ustep, "result")

    graph.addEdge(ustep, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    var pass = false
    try {
      graph.close()
    } catch {
      case _: Throwable => pass = true
    }

    assert(pass)
  }

  "An until " should " iterate at least once" in {
    val graph = new Graph()
    val pipeline = graph.addPipeline()
    val p1       = pipeline.addAtomic(new Producer(List(0)), "p1")

    val comp = new PrimitiveItemComparator()

    val ustep    = pipeline.addUntil(comp)
    val decr     = ustep.addAtomic(new Decrement(), "decr")

    graph.addEdge(p1, "result", ustep, "source")
    graph.addEdge(ustep, "current", decr, "source")
    graph.addEdge(decr, "result", ustep, "test")
    graph.addEdge(decr, "result", ustep, "result")

    graph.addEdge(ustep, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == -1)
  }
}