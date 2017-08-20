package com.jafpl.test

import com.jafpl.graph.Graph
import com.jafpl.io.BufferConsumer
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{Identity, Producer, RaiseError}
import org.scalatest.FlatSpec

class TryCatchSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "A try-catch where the try works " should " succeed" in {
    val bc = new BufferConsumer()

    val graph = new Graph()
    val pipeline = graph.addPipeline()
    val p1       = graph.addAtomic(new Producer(List("doc1")), "p1")
    val p2       = graph.addAtomic(new Producer(List("doc2")), "p2")
    val p3       = graph.addAtomic(new Producer(List("doc3")), "p3")

    val trycatch = graph.addTryCatch("trycatch")
    val try1     = trycatch.addTry("try")
    val ident    = try1.addAtomic(new Identity(), "ident")
    val catch1   = trycatch.addCatch("catch1", List("e1","e2"))
    val ident1   = catch1.addAtomic(new Identity(), "ident1")
    val catch2   = trycatch.addCatch("catch2")
    val ident2   = catch2.addAtomic(new Identity(), "ident2")
    val consumer = graph.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", ident, "source")
    graph.addEdge(ident, "result", try1.end, "result")
    graph.addEdge(try1, "result", trycatch.end, "result")

    graph.addEdge(p2, "result", ident1, "source")
    graph.addEdge(ident1, "result", catch1.end, "result")
    graph.addEdge(catch1, "result", trycatch.end, "result")

    graph.addEdge(p3, "result", ident2, "source")
    graph.addEdge(ident2, "result", catch2.end, "result")
    graph.addEdge(catch2, "result", trycatch.end, "result")

    graph.addEdge(trycatch, "result", pipeline.end, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "doc1")
  }

  "A try-catch " should " match on the code" in {
    val bc = new BufferConsumer()

    val graph = new Graph()
    val pipeline = graph.addPipeline()
    val p1       = graph.addAtomic(new Producer(List("doc1")), "p1")
    val p2       = graph.addAtomic(new Producer(List("doc2")), "p2")
    val p3       = graph.addAtomic(new Producer(List("doc3")), "p3")

    val trycatch = graph.addTryCatch("trycatch")
    val try1     = trycatch.addTry("try")
    val ident    = try1.addAtomic(new RaiseError("e2"), "e2")
    val catch1   = trycatch.addCatch("catch1", List("e1","e2"))
    val ident1   = catch1.addAtomic(new Identity(), "ident1")
    val catch2   = trycatch.addCatch("catch2")
    val ident2   = catch2.addAtomic(new Identity(), "ident2")
    val consumer = graph.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", ident, "source")
    graph.addEdge(ident, "result", try1.end, "result")
    graph.addEdge(try1, "result", trycatch.end, "result")

    graph.addEdge(p2, "result", ident1, "source")
    graph.addEdge(ident1, "result", catch1.end, "result")
    graph.addEdge(catch1, "result", trycatch.end, "result")

    graph.addEdge(p3, "result", ident2, "source")
    graph.addEdge(ident2, "result", catch2.end, "result")
    graph.addEdge(catch2, "result", trycatch.end, "result")

    graph.addEdge(trycatch, "result", pipeline.end, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "doc2")
  }

  "A try-catch " should " use the generic catch if no codes match" in {
    val bc = new BufferConsumer()

    val graph = new Graph()
    val pipeline = graph.addPipeline()
    val p1       = graph.addAtomic(new Producer(List("doc1")), "p1")
    val p2       = graph.addAtomic(new Producer(List("doc2")), "p2")
    val p3       = graph.addAtomic(new Producer(List("doc3")), "p3")

    val trycatch = graph.addTryCatch("trycatch")
    val try1     = trycatch.addTry("try")
    val ident    = try1.addAtomic(new RaiseError("e3"), "e3")
    val catch1   = trycatch.addCatch("catch1", List("e1","e2"))
    val ident1   = catch1.addAtomic(new Identity(), "ident1")
    val catch2   = trycatch.addCatch("catch2")
    val ident2   = catch2.addAtomic(new Identity(), "ident2")
    val consumer = graph.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", ident, "source")
    graph.addEdge(ident, "result", try1.end, "result")
    graph.addEdge(try1, "result", trycatch.end, "result")

    graph.addEdge(p2, "result", ident1, "source")
    graph.addEdge(ident1, "result", catch1.end, "result")
    graph.addEdge(catch1, "result", trycatch.end, "result")

    graph.addEdge(p3, "result", ident2, "source")
    graph.addEdge(ident2, "result", catch2.end, "result")
    graph.addEdge(catch2, "result", trycatch.end, "result")

    graph.addEdge(trycatch, "result", pipeline.end, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "doc3")
  }

  "A try-catch " should " fail if no catches match" in {
    val bc = new BufferConsumer()

    val graph = new Graph()
    val pipeline = graph.addPipeline()
    val p1       = graph.addAtomic(new Producer(List("doc1")), "p1")
    val p2       = graph.addAtomic(new Producer(List("doc2")), "p2")
    val p3       = graph.addAtomic(new Producer(List("doc3")), "p3")

    val trycatch = graph.addTryCatch("trycatch")
    val try1     = trycatch.addTry("try")
    val ident    = try1.addAtomic(new RaiseError("e4"))
    val catch1   = trycatch.addCatch("catch1", List("e1","e2"))
    val ident1   = catch1.addAtomic(new Identity())
    val catch2   = trycatch.addCatch("catch2", List("e3"))
    val ident2   = catch2.addAtomic(new Identity())
    val consumer = graph.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", ident, "source")
    graph.addEdge(ident, "result", try1.end, "result")
    graph.addEdge(try1, "result", trycatch.end, "result")

    graph.addEdge(p2, "result", ident1, "source")
    graph.addEdge(ident1, "result", catch1.end, "result")
    graph.addEdge(catch1, "result", trycatch.end, "result")

    graph.addEdge(p3, "result", ident2, "source")
    graph.addEdge(ident2, "result", catch2.end, "result")
    graph.addEdge(catch2, "result", trycatch.end, "result")

    graph.addEdge(trycatch, "result", pipeline.end, "result")
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