package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.io.BufferConsumer
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{Identity, Manifold, Producer}
import org.scalatest.flatspec.AnyFlatSpec

class ContainerSpec extends AnyFlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration(true)
  val jafpl = Jafpl.newInstance()
  jafpl.traceEventManager.traceEnabled("ALL")

  "Containers " should " allow unread inputs" in {
    val graph     = jafpl.newGraph()

    val pipeline  = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1        = pipeline.addAtomic(new Producer(List("doc1")), "p1")
    val group     = pipeline.addGroup("group", Manifold.ALLOW_ANY)
    val p2        = group.addAtomic(new Producer(List("doc2")), "p2")
    val ident     = group.addAtomic(new Identity(), "ident")

    graph.addEdge(p1, "result", group, "fred")
    graph.addEdge(p2, "result", ident, "source")
    graph.addEdge(ident, "result", group, "result")
    graph.addEdge(group, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)

    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "doc2")
  }

  "Containers " should " allow unread outputs" in {
    val graph     = jafpl.newGraph()

    val pipeline  = graph.addPipeline(Manifold.ALLOW_ANY)
    val group     = pipeline.addGroup("group", Manifold.ALLOW_ANY)
    val p1        = group.addAtomic(new Producer(List("doc1")), "p1")
    val p2        = group.addAtomic(new Producer(List("doc2")), "p1")
    val ident     = group.addAtomic(new Identity(), "ident")

    graph.addEdge(p1, "result", ident, "source")
    graph.addEdge(p2, "result", group, "fred")
    graph.addEdge(ident, "result", group, "result")
    graph.addEdge(group, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)

    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "doc1")
  }

}