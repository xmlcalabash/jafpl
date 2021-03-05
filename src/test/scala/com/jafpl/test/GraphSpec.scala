package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.exceptions.JafplException
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.steps.{Identity, Manifold, Producer, Sink}
import org.scalatest.flatspec.AnyFlatSpec

class GraphSpec extends AnyFlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "A simple graph " should " compile" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val producer = pipeline.addAtomic(new Producer(List("DOCUMENT")), "producer")
    val ident1 = pipeline.addAtomic(new Identity(), "ident1")
    val ident2 = pipeline.addAtomic(new Identity(), "ident2")
    val consumer = pipeline.addAtomic(new Sink(), "consumer")

    graph.addEdge(producer, "result", pipeline, "source")
    graph.addEdge(pipeline, "source", ident1, "source")
    graph.addEdge(ident1, "result", ident2, "source")
    graph.addEdge(ident2, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    graph.close()
    assert(graph.valid)
  }

  "Containers " should " be able to read from nodes outside them" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val producer = pipeline.addAtomic(new Producer(List("DOCUMENT")), "producer")
    val group    = pipeline.addGroup("group", Manifold.ALLOW_ANY)
    val inner    = group.addAtomic(new Identity(), "ident")
    val consumer = pipeline.addAtomic(new Sink(), "consumer")

    graph.addEdge(producer, "result", inner, "source")
    graph.addEdge(inner, "result", group, "result")
    graph.addEdge(group, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    graph.close()
    assert(graph.valid)
  }

  "Steps " should " not be able to read from nodes inside containers" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val producer = pipeline.addAtomic(new Producer(List("DOCUMENT")), "producer")
    val group    = pipeline.addGroup("group", Manifold.ALLOW_ANY)
    val inner    = group.addAtomic(new Identity(), "inner")
    val outer    = pipeline.addAtomic(new Identity(), "outer")
    val consumer = pipeline.addAtomic(new Sink(), "consumer")

    graph.addEdge(producer, "result", inner, "source")
    graph.addEdge(inner, "result", group, "result")
    graph.addEdge(group, "result", pipeline, "result")
    graph.addEdge(inner, "result", outer, "source")
    graph.addEdge(outer, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    var pass = false
    try {
      graph.close()
    } catch {
      case _: Throwable => pass = true
    }

    assert(pass)
  }

  "Nodes " should " be from the same graph" in {
    val jafpl     = Jafpl.newInstance()
    val graph1    = jafpl.newGraph()
    val graph2    = jafpl.newGraph()

    val pipeline1 = graph1.addPipeline(Manifold.ALLOW_ANY)
    val ident1 = pipeline1.addAtomic(new Identity(), "ident1")

    val pipeline2 = graph2.addPipeline(Manifold.ALLOW_ANY)
    val ident2 = pipeline2.addAtomic(new Identity(), "ident2")

    var pass = false
    try {
      graph1.addEdge(ident1, "result", ident2, "source")
      graph1.close()
    } catch {
      case eg: JafplException => pass = true
    }

    assert(pass)
  }

  "A closed graph " should " not be updatable" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val producer = pipeline.addAtomic(new Producer(List("DOCUMENT")), "producer")
    val ident1 = pipeline.addAtomic(new Identity(), "ident1")
    val ident2 = pipeline.addAtomic(new Identity(), "ident2")
    val consumer = pipeline.addAtomic(new Sink(), "consumer")

    graph.addEdge(producer, "result", pipeline, "source")
    graph.addEdge(pipeline, "source", ident1, "source")
    graph.addEdge(ident1, "result", ident2, "source")
    graph.addEdge(ident2, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    graph.close()

    var pass = false
    try {
      graph.addEdge(ident1, "result", ident2, "source")
    } catch {
      case ge: JafplException => pass = true
    }

    assert(pass)
  }

  "No loops " should " be allowed" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val producer = pipeline.addAtomic(new Producer(List("DOCUMENT")), "producer")
    val ident1 = pipeline.addAtomic(new Identity(), "ident1")
    val ident2 = pipeline.addAtomic(new Identity(), "ident2")
    val consumer = pipeline.addAtomic(new Sink(), "consumer")

    graph.addEdge(producer, "result", pipeline, "source")
    graph.addEdge(pipeline, "source", ident1, "source")

    graph.addEdge(ident1, "result", ident2, "source")
    graph.addEdge(ident2, "result", ident1, "source")

    graph.addEdge(pipeline, "result", consumer, "source")

    var pass = false
    try {
      graph.close()
    } catch {
      case _: Throwable => pass = true
    }

    assert(pass && !graph.valid)
  }
}