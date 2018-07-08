package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.exceptions.GraphException
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Identity, Producer, Sink, Sleep}
import org.scalatest.FlatSpec

class JoinerSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "The mixed joiner pipeline " should " run" in {
    val graph = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline()
    val p1 = pipeline.addAtomic(new Producer(List("A1","A2")), "Adocs")
    val p2 = pipeline.addAtomic(new Producer(List("B1","B2")), "Bdocs")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", pipeline, "result")
    graph.addEdge(p2, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 4)
  }

  "The ordered joiner pipeline " should " run" in {
    val graph = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline()
    val p1 = pipeline.addAtomic(new Producer(List("A1","A2")), "Adocs")
    val p2 = pipeline.addAtomic(new Producer(List("B1","B2")), "Bdocs")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addOrderedEdge(p1, "result", pipeline, "result")
    graph.addEdge(p2, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 4)
    assert(bc.items(0) == "A1")
    assert(bc.items(1) == "A2")
    assert(bc.items(2) == "B1")
    assert(bc.items(3) == "B2")
  }

  "Attempting to make any edge but the first a priority edge " should " fail" in {
    val graph = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline()
    val p1 = pipeline.addAtomic(new Producer(List("A1","A2")), "Adocs")
    val p2 = pipeline.addAtomic(new Producer(List("B1","B2")), "Bdocs")
    val consumer = pipeline.addAtomic(bc, "consumer")

    var pass = false
    try {
      graph.addEdge(p1, "result", pipeline, "result")
      graph.addPriorityEdge(p2, "result", pipeline, "result")
      graph.addEdge(pipeline, "result", consumer, "source")
    } catch {
      case g: GraphException => pass = true
    }

    assert(pass)
  }

  "With input on P1, a priority joiner " should " return only A1 and A2" in {
    val graph = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline()
    val p1 = pipeline.addAtomic(new Producer(List("A1","A2")), "Adocs")
    val p2 = pipeline.addAtomic(new Producer(List("B1","B2")), "Bdocs")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addPriorityEdge(p1, "result", pipeline, "result")
    graph.addEdge(p2, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 2)
    assert(bc.items(0) == "A1")
    assert(bc.items(1) == "A2")
  }

  "With no input on P1, a priority joiner " should " return only B1 and B2" in {
    val graph = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline()
    val p1 = pipeline.addAtomic(new Producer(List()), "Adocs")
    val p2 = pipeline.addAtomic(new Producer(List("B1","B2")), "Bdocs")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addPriorityEdge(p1, "result", pipeline, "result")
    graph.addEdge(p2, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 2)
    assert(bc.items(0) == "B1")
    assert(bc.items(1) == "B2")
  }

  "With input on P1, a priority joiner " should " return only A1 and A2 even if there are more inputs" in {
    val graph = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline()
    val p1 = pipeline.addAtomic(new Producer(List("A1","A2")), "Adocs")
    val p2 = pipeline.addAtomic(new Producer(List("B1","B2")), "Bdocs")
    val p3 = pipeline.addAtomic(new Producer(List("C1","C2")), "Cdocs")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addPriorityEdge(p1, "result", pipeline, "result")
    graph.addEdge(p2, "result", pipeline, "result")
    graph.addEdge(p3, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 2)
    assert(bc.items(0) == "A1")
    assert(bc.items(1) == "A2")
  }

  "With no input on P1, a priority joiner " should " return B* and C*" in {
    val graph = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline()
    val p1 = pipeline.addAtomic(new Producer(List()), "Adocs")
    val p2 = pipeline.addAtomic(new Producer(List("B1","B2")), "Bdocs")
    val p3 = pipeline.addAtomic(new Producer(List("C1","C2")), "Cdocs")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addPriorityEdge(p1, "result", pipeline, "result")
    graph.addEdge(p2, "result", pipeline, "result")
    graph.addEdge(p3, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 4)
    assert(bc.items(0) == "B1")
    assert(bc.items(1) == "B2")
    assert(bc.items(2) == "C1")
    assert(bc.items(3) == "C2")
  }
}