package com.jafpl.test

import com.jafpl.graph.Graph
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Count, Identity, Producer, Sink}
import org.scalatest.FlatSpec

class ForEachSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "A for-each " should " iterate" in {
    val graph = new Graph()

    val pipeline = graph.addPipeline()
    val producer = pipeline.addAtomic(new Producer(List("1", "2", "3")), "producer")
    val forEach  = pipeline.addForEach("for-each")
    val ident    = forEach.addAtomic(new Identity(), "ident")

    val bc = new BufferSink()
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(producer, "result", forEach, "source")
    graph.addEdge(forEach, "source", ident, "source")
    graph.addEdge(ident, "result", forEach, "result")
    graph.addEdge(forEach, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    var count = 1
    for (buf <- bc.items) {
      assert(buf.toString == count.toString)
      count += 1
    }
    assert(count == 4)
  }

  "A for-each with three inputs " should " output 3 documents" in {
    val graph = new Graph()

    val pipeline = graph.addPipeline()
    val producer = pipeline.addAtomic(new Producer(List("1", "2", "3")), "producer")
    val forEach  = pipeline.addForEach("for-each")
    val ident    = forEach.addAtomic(new Identity(), "ident")

    val count    = pipeline.addAtomic(new Count(), "count")

    val bc = new BufferSink()
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(producer, "result", forEach, "source")
    graph.addEdge(forEach, "source", ident, "source")
    graph.addEdge(ident, "result", forEach, "result")
    graph.addEdge(forEach, "result", count, "source")
    graph.addEdge(count, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == 3)
  }

  "Inputs that cross a for-each " should " be buffered" in {
    val graph = new Graph()

    val pipeline = graph.addPipeline()
    val cprod    = pipeline.addAtomic(new Producer(List("1", "2", "3", "4")), "count_producer")
    val lprod    = pipeline.addAtomic(new Producer(List("1", "2", "3")), "loop_producer")
    val count    = pipeline.addAtomic(new Count(), "count")
    val forEach  = pipeline.addForEach("for-each")
    val sink     = forEach.addAtomic(new Sink(), "sink")
    val ident    = forEach.addAtomic(new Identity(), "ident")

    val bc = new BufferSink()
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(cprod, "result", count, "source")
    graph.addEdge(count, "result", ident, "source")

    graph.addEdge(lprod, "result", forEach, "source")
    graph.addEdge(forEach, "source", sink, "source")
    graph.addEdge(ident, "result", forEach, "result")
    graph.addEdge(forEach, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 3)
    for (item <- bc.items) {
      assert(item == 4)
    }
  }

}