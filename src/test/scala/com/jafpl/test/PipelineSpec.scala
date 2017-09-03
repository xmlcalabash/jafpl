package com.jafpl.test

import com.jafpl.graph.Graph
import com.jafpl.io.BufferConsumer
import com.jafpl.messages.Metadata
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{Identity, Producer}
import org.scalatest.FlatSpec

class PipelineSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  behavior of "A pipeline"

  it should "allow multiple inputs" in {
    val graph = new Graph()

    val pipeline  = graph.addPipeline()
    val ident     = pipeline.addAtomic(new Identity(), "ident")

    graph.addEdge(pipeline, "source1", ident, "source")
    graph.addEdge(pipeline, "source2", ident, "source")
    graph.addEdge(ident, "result", pipeline, "result")

    graph.addInput(pipeline, "source1")
    graph.addInput(pipeline, "source2")
    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.inputs("source1").receive("source1", "Hello", Metadata.BLANK)
    runtime.inputs("source2").receive("source", "World", Metadata.BLANK)

    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)

    runtime.run()

    assert(bc.items.size == 2)
    assert(((bc.items(0) == "Hello") && (bc.items(1) == "World"))
      || ((bc.items(0) == "World") && (bc.items(1) == "Hello")))

  }

  it should "allow multiple outputs" in {
    val graph = new Graph()

    val pipeline  = graph.addPipeline()
    val producer1 = pipeline.addAtomic(new Producer("ONE"), "producer1")
    val producer2 = pipeline.addAtomic(new Producer("TWO"), "producer2")

    graph.addOutput(pipeline, "result1")
    graph.addOutput(pipeline, "result2")

    graph.addEdge(producer1, "result", pipeline, "result1")
    graph.addEdge(producer2, "result", pipeline, "result2")

    val runtime = new GraphRuntime(graph, runtimeConfig)

    val bc1 = new BufferConsumer()
    runtime.outputs("result1").setConsumer(bc1)
    val bc2 = new BufferConsumer()
    runtime.outputs("result2").setConsumer(bc2)

    runtime.run()

    assert(bc1.items.size == 1)
    assert(bc1.items.head == "ONE")

    assert(bc2.items.size == 1)
    assert(bc2.items.head == "TWO")
  }

  it should "allow unread inputs" in {
    val graph = new Graph()

    val pipeline  = graph.addPipeline()
    val p1        = pipeline.addAtomic(new Producer(List("doc1")), "p1")
    val ident     = pipeline.addAtomic(new Identity(), "ident")

    graph.addEdge(p1, "result", ident, "source")
    graph.addEdge(ident, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")
    graph.addInput(pipeline, "source")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "doc1")
  }

  it should "allow unread outputs" in {
    val graph = new Graph()

    val pipeline  = graph.addPipeline()
    val p1        = pipeline.addAtomic(new Producer(List("doc1")), "p1")
    val p2        = pipeline.addAtomic(new Producer(List("doc2")), "p1")
    val ident     = pipeline.addAtomic(new Identity(), "ident")

    graph.addEdge(p1, "result", ident, "source")
    graph.addEdge(p2, "result", pipeline, "fred")
    graph.addEdge(ident, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "doc1")
  }

  it should " be abortable" in {
    val graph = new Graph()

    val pipeline  = graph.addPipeline()
    val p1        = pipeline.addAtomic(new Producer(List("doc1")), "p1")
    val p2        = pipeline.addAtomic(new Producer(List("doc2")), "p1")
    val ident     = pipeline.addAtomic(new Identity(), "ident")

    graph.addEdge(p1, "result", ident, "source")
    graph.addEdge(p2, "result", pipeline, "fred")
    graph.addEdge(ident, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)

    var pass = true
    try {
      // This will throw an exception because there's no "fred" output
      val bc2 = new BufferConsumer()
      runtime.outputs("fred").setConsumer(bc2)
      runtime.run()
      pass = false
    } catch {
      case t: Throwable =>
        runtime.stop()
    }

    assert(pass)
  }

}