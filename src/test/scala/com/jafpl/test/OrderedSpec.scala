package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.io.BufferConsumer
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{Identity, Manifold, Producer}
import org.scalatest.FlatSpec

class OrderedSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "A pipeline with an ordered joiner " should " join inputs in the right order" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)

    val p1  = pipeline.addAtomic(new Producer("One"), "one")
    val p2a = pipeline.addAtomic(new Producer("TwoA"), "twoA")
    val p2b = pipeline.addAtomic(new Producer("TwoB"), "twoB")
    val p3  = pipeline.addAtomic(new Producer("Three"), "three")

    val join = pipeline.addAtomic(new Identity(), "identityA")
    val identity = pipeline.addAtomic(new Identity(), "identity")

    graph.addOrderedEdge(p2a, "result", join, "source")
    graph.addEdge(p2b, "result", join, "source")

    graph.addOrderedEdge(p1, "result", identity, "source")
    graph.addOrderedEdge(join, "result", identity, "source")
    graph.addOrderedEdge(p3, "result", identity, "source")

    graph.addEdge(identity, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)

    runtime.run()

    assert(bc.items.length == 4)
    assert(bc.items.head == "One")
    assert(bc.items(1) == "TwoA")
    assert(bc.items(2) == "TwoB")
    assert(bc.items(3) == "Three")
  }

  "Splitting an ordered joiner " should " join inputs in the right order" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)

    val p1 = pipeline.addAtomic(new Producer("One"), "one")
    val p2 = pipeline.addAtomic(new Producer("Two"), "twoA")

    val identity = pipeline.addAtomic(new Identity(), "identity")

    graph.addOrderedEdge(p1, "result", identity, "source")
    graph.addOrderedEdge(p2, "result", identity, "source")
    graph.addOrderedEdge(p1, "result", identity, "source")
    graph.addOrderedEdge(p2, "result", identity, "source")

    graph.addEdge(identity, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)

    runtime.run()

    print(bc.items)

    assert(bc.items.length == 4)
    assert(bc.items.head == "One")
    assert(bc.items(1) == "Two")
    assert(bc.items(2) == "One")
    assert(bc.items(3) == "Two")
  }
}