package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.io.BufferConsumer
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Count, ExceptionTranslator, Identity, Manifold, Producer, RaiseError, RaiseErrorException}
import org.scalatest.FlatSpec

class TryCatchSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  behavior of "A try/catch"

  it should "succeed if the try branch succeeds" in {
    val bc = new BufferSink()

    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1       = pipeline.addAtomic(new Producer(List("doc1")), "p1")
    val p2       = pipeline.addAtomic(new Producer(List("doc2")), "p2")
    val p3       = pipeline.addAtomic(new Producer(List("doc3")), "p3")

    val trycatch = pipeline.addTryCatch("trycatch")
    val try1     = trycatch.addTry("try")
    val ident    = try1.addAtomic(new Identity(), "ident")
    val catch1   = trycatch.addCatch("catch1", List("e1","e2"))
    val ident1   = catch1.addAtomic(new Identity(), "ident1")
    val catch2   = trycatch.addCatch("catch2")
    val ident2   = catch2.addAtomic(new Identity(), "ident2")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", ident, "source")
    graph.addEdge(ident, "result", try1, "result")
    graph.addEdge(try1, "result", trycatch, "result")

    graph.addEdge(p2, "result", ident1, "source")
    graph.addEdge(ident1, "result", catch1, "result")
    graph.addEdge(catch1, "result", trycatch, "result")

    graph.addEdge(p3, "result", ident2, "source")
    graph.addEdge(ident2, "result", catch2, "result")
    graph.addEdge(catch2, "result", trycatch, "result")

    graph.addEdge(trycatch, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "doc1")
  }

  it should "run the catch that matches the error code" in {
    val bc = new BufferSink()

    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1       = pipeline.addAtomic(new Producer(List("doc1")), "p1")
    val p2       = pipeline.addAtomic(new Producer(List("doc2")), "p2")
    val p3       = pipeline.addAtomic(new Producer(List("doc3")), "p3")

    val trycatch = pipeline.addTryCatch("trycatch")
    val try1     = trycatch.addTry("try")
    val ident    = try1.addAtomic(new RaiseError("e2"), "e2")
    val catch1   = trycatch.addCatch("catch1", List("e1","e2"))
    val ident1   = catch1.addAtomic(new Identity(), "ident1")
    val catch2   = trycatch.addCatch("catch2")
    val ident2   = catch2.addAtomic(new Identity(), "ident2")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", ident, "source")
    graph.addEdge(ident, "result", try1, "result")
    graph.addEdge(try1, "result", trycatch, "result")

    graph.addEdge(p2, "result", ident1, "source")
    graph.addEdge(ident1, "result", catch1, "result")
    graph.addEdge(catch1, "result", trycatch, "result")

    graph.addEdge(p3, "result", ident2, "source")
    graph.addEdge(ident2, "result", catch2, "result")
    graph.addEdge(catch2, "result", trycatch, "result")

    graph.addEdge(trycatch, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "doc2")
  }

  it should "run the generic catch if no codes match" in {
    val bc = new BufferSink()

    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1       = pipeline.addAtomic(new Producer(List("doc1")), "p1")
    val p2       = pipeline.addAtomic(new Producer(List("doc2")), "p2")
    val p3       = pipeline.addAtomic(new Producer(List("doc3")), "p3")

    val trycatch = pipeline.addTryCatch("trycatch")
    val try1     = trycatch.addTry("try")
    val ident    = try1.addAtomic(new RaiseError("e3"), "e3")
    val catch1   = trycatch.addCatch("catch1", List("e1","e2"))
    val ident1   = catch1.addAtomic(new Identity(), "ident1")
    val catch2   = trycatch.addCatch("catch2")
    val ident2   = catch2.addAtomic(new Identity(), "ident2")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", ident, "source")
    graph.addEdge(ident, "result", try1, "result")
    graph.addEdge(try1, "result", trycatch, "result")

    graph.addEdge(p2, "result", ident1, "source")
    graph.addEdge(ident1, "result", catch1, "result")
    graph.addEdge(catch1, "result", trycatch, "result")

    graph.addEdge(p3, "result", ident2, "source")
    graph.addEdge(ident2, "result", catch2, "result")
    graph.addEdge(catch2, "result", trycatch, "result")

    graph.addEdge(trycatch, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "doc3")
  }

  it should "fail if no catches match" in {
    val bc = new BufferSink()

    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1       = pipeline.addAtomic(new Producer(List("doc1")), "p1")
    val p2       = pipeline.addAtomic(new Producer(List("doc2")), "p2")
    val p3       = pipeline.addAtomic(new Producer(List("doc3")), "p3")

    val trycatch = pipeline.addTryCatch("trycatch")
    val try1     = trycatch.addTry("try")
    val ident    = try1.addAtomic(new RaiseError("e4"))
    val catch1   = trycatch.addCatch("catch1", List("e1","e2"))
    val ident1   = catch1.addAtomic(new Identity())
    val catch2   = trycatch.addCatch("catch2", List("e3"))
    val ident2   = catch2.addAtomic(new Identity())
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", ident, "source")
    graph.addEdge(ident, "result", try1, "result")
    graph.addEdge(try1, "result", trycatch, "result")

    graph.addEdge(p2, "result", ident1, "source")
    graph.addEdge(ident1, "result", catch1, "result")
    graph.addEdge(catch1, "result", trycatch, "result")

    graph.addEdge(p3, "result", ident2, "source")
    graph.addEdge(ident2, "result", catch2, "result")
    graph.addEdge(catch2, "result", trycatch, "result")

    graph.addEdge(trycatch, "result", pipeline, "result")
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

  it should "run the finally when the try succeeds" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1       = pipeline.addAtomic(new Producer(List("doc1")), "p1")
    val p2       = pipeline.addAtomic(new Producer(List("doc2")), "p2")
    val p3       = pipeline.addAtomic(new Producer(List("doc3")), "p3")

    val trycatch = pipeline.addTryCatch("trycatch")
    val try1     = trycatch.addTry("try")
    val ident    = try1.addAtomic(new Identity(), "ident")
    val catch1   = trycatch.addCatch("catch1", List("e1","e2"))
    val ident1   = catch1.addAtomic(new Identity(), "ident1")
    val catch2   = trycatch.addCatch("catch2")
    val ident2   = catch2.addAtomic(new Identity(), "ident2")
    val fin      = trycatch.addFinally("finally")
    val count    = fin.addAtomic(new Count(), "count")

    graph.addEdge(p1, "result", ident, "source")
    graph.addEdge(ident, "result", try1, "result")
    graph.addEdge(try1, "result", trycatch, "result")

    graph.addEdge(p2, "result", ident1, "source")
    graph.addEdge(ident1, "result", catch1, "result")
    graph.addEdge(catch1, "result", trycatch, "result")

    graph.addEdge(p3, "result", ident2, "source")
    graph.addEdge(ident2, "result", catch2, "result")
    graph.addEdge(catch2, "result", trycatch, "result")

    graph.addEdge(fin, "errors", count, "source")
    graph.addEdge(count, "result", fin, "result")
    graph.addEdge(fin, "result", trycatch, "finally")

    graph.addEdge(trycatch, "result", pipeline, "result")
    graph.addEdge(trycatch, "finally", pipeline, "finally")

    graph.addOutput(pipeline, "result")
    graph.addOutput(pipeline, "finally")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)

    val bc_result = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc_result)

    val bc_finally = new BufferConsumer()
    runtime.outputs("finally").setConsumer(bc_finally)

    runtime.run()

    assert(bc_result.items.size == 1)
    assert(bc_result.items.head == "doc1")
    assert(bc_finally.items.size == 1)
    assert(bc_finally.items.head == 0)
  }

  it should "run the finally after catching an error" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1       = pipeline.addAtomic(new Producer(List("doc1")), "p1")
    val p2       = pipeline.addAtomic(new Producer(List("doc2")), "p2")
    val p3       = pipeline.addAtomic(new Producer(List("doc3")), "p3")

    val trycatch = pipeline.addTryCatch("trycatch")
    val try1     = trycatch.addTry("try")
    val raiseerr = try1.addAtomic(new RaiseError("e1"), "error")
    val catch1   = trycatch.addCatch("catch1", List("e1","e2"))
    val ident1   = catch1.addAtomic(new Identity(), "ident1")
    val catch2   = trycatch.addCatch("catch2")
    val ident2   = catch2.addAtomic(new Identity(), "ident2")
    val fin      = trycatch.addFinally("finally")
    val count    = fin.addAtomic(new Count(), "count")

    graph.addEdge(p1, "result", raiseerr, "source")
    graph.addEdge(raiseerr, "result", try1, "result")
    graph.addEdge(try1, "result", trycatch, "result")

    graph.addEdge(p2, "result", ident1, "source")
    graph.addEdge(ident1, "result", catch1, "result")
    graph.addEdge(catch1, "result", trycatch, "result")

    graph.addEdge(p3, "result", ident2, "source")
    graph.addEdge(ident2, "result", catch2, "result")
    graph.addEdge(catch2, "result", trycatch, "result")

    graph.addEdge(fin, "errors", count, "source")
    graph.addEdge(count, "result", fin, "result")
    graph.addEdge(fin, "result", trycatch, "finally")

    graph.addEdge(trycatch, "result", pipeline, "result")
    graph.addEdge(trycatch, "finally", pipeline, "finally")

    graph.addOutput(pipeline, "result")
    graph.addOutput(pipeline, "finally")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)

    val bc_result = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc_result)

    val bc_finally = new BufferConsumer()
    runtime.outputs("finally").setConsumer(bc_finally)

    runtime.run()

    assert(bc_result.items.size == 1)
    assert(bc_result.items.head == "doc2")
    assert(bc_finally.items.size == 1)
    assert(bc_finally.items.head == 1)
  }

  it should "be possible to read the error port" in {
    val bc = new BufferSink()

    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1       = pipeline.addAtomic(new Producer(List("doc1")), "p1")
    //val px       = pipeline.addAtomic(new Producer(List("doc2")), "p2")

    val trycatch = pipeline.addTryCatch("trycatch")
    val try1     = trycatch.addTry("try")
    val raise    = try1.addAtomic(new RaiseError("e3"), "e3")
    val catchx   = trycatch.addCatch("catchx")
    val identx   = catchx.addAtomic(new Identity(), "identx")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", raise, "source")
    graph.addEdge(raise, "result", try1, "result")
    graph.addEdge(try1, "result", trycatch, "result")

    graph.addEdge(catchx, "errors", identx, "source")
    graph.addEdge(identx, "result", catchx, "result")
    graph.addEdge(catchx, "result", trycatch, "result")

    graph.addEdge(trycatch, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head.isInstanceOf[RaiseErrorException])
  }

  it should "be possible to add a translator for the errors" in {
    val bc = new BufferSink()

    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1       = pipeline.addAtomic(new Producer(List("doc1")), "p1")
    //val px       = pipeline.addAtomic(new Producer(List("doc2")), "p2")

    val trycatch = pipeline.addTryCatch("trycatch")
    val try1     = trycatch.addTry("try")
    val raise    = try1.addAtomic(new RaiseError("e3"), "e3")
    val catchx   = trycatch.addCatch("catchx")
    val identx   = catchx.addAtomic(new Identity(), "identx")
    val consumer = pipeline.addAtomic(bc, "consumer")
    val xlate    = catchx.translator = new ExceptionTranslator()

    graph.addEdge(p1, "result", raise, "source")
    graph.addEdge(raise, "result", try1, "result")
    graph.addEdge(try1, "result", trycatch, "result")

    graph.addEdge(catchx, "errors", identx, "source")
    graph.addEdge(identx, "result", catchx, "result")
    graph.addEdge(catchx, "result", trycatch, "result")

    graph.addEdge(trycatch, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "Caught one!")
  }

}