package com.jafpl.test

import com.jafpl.graph.Graph
import com.jafpl.io.BufferConsumer
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Count, Identity, Producer, Sink}
import org.scalatest.FlatSpec

class ForLoopSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  behavior of "A for-loop"

  it should "iterate up" in {
    val graph = new Graph()
    val pipeline = graph.addPipeline("mypipe")
    val forloop  = pipeline.addFor("loop", 1, 10)
    val ident = forloop.addAtomic(new Identity(), "ident")

    graph.addEdge(forloop, "current", ident, "source")
    graph.addEdge(ident, "result", forloop, "result")
    graph.addEdge(forloop, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.run()

    assert(bc.items.size == 10)
    var count = 1
    for (buf <- bc.items) {
      assert(buf.toString == count.toString)
      count += 1
    }
  }

  it should "iterate down" in {
    val graph = new Graph()
    val pipeline = graph.addPipeline("mypipe")
    val forloop  = pipeline.addFor("loop", 20, 1, -2)
    val ident = forloop.addAtomic(new Identity(), "ident")

    graph.addEdge(forloop, "current", ident, "source")
    graph.addEdge(ident, "result", forloop, "result")
    graph.addEdge(forloop, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.run()

    assert(bc.items.size == 10)
    var count = 20
    for (buf <- bc.items) {
      assert(buf.toString == count.toString)
      count -= 2
    }
  }
}