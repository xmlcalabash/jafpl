package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.exceptions.JafplException
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Identity, LiesAboutOutputBindings, Manifold, PortCardinality, Producer}
import org.scalatest.FlatSpec

class CardinalitySpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()
  val oneOutput = new Manifold(Manifold.WILD, Manifold.singlePort("result", PortCardinality.EXACTLY_ONE))

  "Incorrect input cardinalities " should " cause the pipeline to fail" in {
    val graph    = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1       = pipeline.addAtomic(new Producer(List("P1", "P2")), "producer")
    val ident    = pipeline.addAtomic(new Identity(false), "identity")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", ident, "source")
    graph.addEdge(ident, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    var pass = false
    try {
      val runtime = new GraphRuntime(graph, runtimeConfig)
      runtime.run()
    } catch {
      case jex: JafplException => pass = true
    }

    assert(pass)
  }

  "Incorrect output cardinalities " should " cause the pipeline to fail" in {
    val graph    = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1       = pipeline.addAtomic(new Producer(List("P1", "P2")), "producer")
    val liar     = pipeline.addAtomic(new LiesAboutOutputBindings(), "liar")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", liar, "source")
    graph.addEdge(liar, "result", pipeline, "result")
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

  "Cardinalities " should " be enforced on group" in {
    val graph    = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val p1       = pipeline.addAtomic(new Producer(List("P1", "P2", "P3")), "producer")
    val group    = pipeline.addGroup(oneOutput)
    val ident    = group.addAtomic(new Identity(), "identity")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", ident, "source")
    graph.addEdge(ident, "result", group, "result")
    graph.addEdge(group, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    var pass = false
    try {
      val runtime = new GraphRuntime(graph, runtimeConfig)
      runtime.run()
    } catch {
      case jafpl: JafplException =>
        pass = jafpl.code == JafplException.CARDINALITY_ERROR
    }

    assert(pass)
  }

  "Cardinalities " should " be enforced on pipeline" in {
    val graph    = Jafpl.newInstance().newGraph()
    val bc = new BufferSink()

    val pipeline = graph.addPipeline(oneOutput)
    val p1       = pipeline.addAtomic(new Producer(List("P1", "P2", "P3")), "producer")
    val group    = pipeline.addGroup(Manifold.ALLOW_ANY)
    val ident    = group.addAtomic(new Identity(), "identity")
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(p1, "result", ident, "source")
    graph.addEdge(ident, "result", group, "result")
    graph.addEdge(group, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    var pass = false
    try {
      val runtime = new GraphRuntime(graph, runtimeConfig)
      runtime.run()
    } catch {
      case jafpl: JafplException =>
        pass = jafpl.code == JafplException.CARDINALITY_ERROR
    }

    assert(pass)
  }

  "Cardinalities" should " be enforced on a for-each" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline      = graph.addPipeline(Manifold.ALLOW_ANY)
    val producer      = pipeline.addAtomic(new Producer(List("1", "2", "3")), "producer")
    val outerForEach  = pipeline.addForEach("o-for-each", new Manifold(Manifold.WILD, Manifold.singlePort("result", 0, 2)))
    val ident         = outerForEach.addAtomic(new Identity(), "ident")

    val bc = new BufferSink()
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(producer, "result", outerForEach, "source")
    graph.addEdge(outerForEach, "current", ident, "source")
    graph.addEdge(ident, "result", outerForEach, "result")
    graph.addEdge(outerForEach, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    var pass = false
    try {
      val runtime = new GraphRuntime(graph, runtimeConfig)
      runtime.run()
    } catch {
      case jafpl: JafplException =>
        pass = jafpl.code == JafplException.CARDINALITY_ERROR
    }

    assert(pass)
  }

  "Cardinalities" should " be reset on nested for-each loops" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline      = graph.addPipeline(Manifold.ALLOW_ANY)
    val producer      = pipeline.addAtomic(new Producer(List("1", "2", "3")), "producer")
    val outerForEach  = pipeline.addForEach("o-for-each", new Manifold(Manifold.WILD, Manifold.singlePort("result", 0, 3)))
    val innerForEach  = outerForEach.addForEach("i-for-each", new Manifold(Manifold.WILD, Manifold.singlePort("result", 0, 1)))
    val ident         = innerForEach.addAtomic(new Identity(), "ident")

    val bc = new BufferSink()
    val consumer = pipeline.addAtomic(bc, "consumer")

    graph.addEdge(producer, "result", outerForEach, "source")
    graph.addEdge(outerForEach, "current", innerForEach, "source")
    graph.addEdge(innerForEach, "current", ident, "source")
    graph.addEdge(ident, "result", innerForEach, "result")
    graph.addEdge(innerForEach, "result", outerForEach, "result")
    graph.addEdge(outerForEach, "result", pipeline, "result")
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
}