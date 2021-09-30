package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.io.BufferConsumer
import com.jafpl.primitive.{PrimitiveItemComparator, PrimitiveRuntimeConfiguration}
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{Decrement, Manifold, Producer}
import org.scalatest.flatspec.AnyFlatSpec

class UntilSpec extends AnyFlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "An until " should " iterate until finished and return all" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1       = pipeline.addAtomic(new Producer(List(7)), "p1")

    val comp = new PrimitiveItemComparator()

    val ustep    = pipeline.addUntil(comp, true, Manifold.ALLOW_ANY)
    val decr     = ustep.addAtomic(new Decrement(), "decr")

    graph.addEdge(p1, "result", ustep, "source")
    graph.addEdge(ustep, "current", decr, "source")
    graph.addEdge(decr, "result", ustep, "test")
    graph.addEdge(decr, "result", ustep, "result")

    graph.addEdge(ustep, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")
    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

    assert(bc.items.size == 7)
  }

  "An until " should " iterate until finished and return all on multiple ports" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1       = pipeline.addAtomic(new Producer(List(7)), "p1")

    val comp = new PrimitiveItemComparator()

    val ustep    = pipeline.addUntil(comp, true, Manifold.ALLOW_ANY)
    val decr     = ustep.addAtomic(new Decrement(), "decr")

    graph.addEdge(p1, "result", ustep, "source")
    graph.addEdge(ustep, "current", decr, "source")
    graph.addEdge(decr, "result", ustep, "test")
    graph.addEdge(decr, "result", ustep, "result1")
    graph.addEdge(decr, "result", ustep, "result2")

    graph.addEdge(ustep, "result1", pipeline, "result1")
    graph.addEdge(ustep, "result2", pipeline, "result2")

    graph.addOutput(pipeline, "result1")
    graph.addOutput(pipeline, "result2")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc1 = new BufferConsumer()
    val bc2 = new BufferConsumer()
    runtime.outputs("result1").setConsumer(bc1)
    runtime.outputs("result2").setConsumer(bc2)
    runtime.runSync()

    assert(bc1.items.size == 7)
    assert(bc2.items.size == 7)
  }

  "An until " should " iterate until finished and return 1" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1       = pipeline.addAtomic(new Producer(List(7)), "p1")

    val comp = new PrimitiveItemComparator()

    val ustep    = pipeline.addUntil(comp, false, Manifold.ALLOW_ANY)
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
    runtime.runSync()

    assert(bc.items.size == 1)
  }

  "An until " should " require a test" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1       = pipeline.addAtomic(new Producer(List(7)), "p1")

    val comp = new PrimitiveItemComparator()

    val ustep    = pipeline.addUntil(comp, false, Manifold.ALLOW_ANY)
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
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1       = pipeline.addAtomic(new Producer(List(0)), "p1")

    val comp = new PrimitiveItemComparator()

    val ustep    = pipeline.addUntil(comp, true, Manifold.ALLOW_ANY)
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
    runtime.runSync()

    assert(bc.items.size == 1)
    assert(bc.items.head == -1)
  }
}