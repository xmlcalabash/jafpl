package com.jafpl.test

import com.jafpl.exceptions.GraphException
import com.jafpl.graph.Graph
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.steps.{Identity, Producer, Sink}
import org.scalatest.FlatSpec

class GraphSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "A simple graph " should " compile" in {
    val graph = new Graph()

    val pipeline = graph.addPipeline()
    val producer = graph.addAtomic(new Producer(List("DOCUMENT")), "producer")
    val ident1 = pipeline.addAtomic(new Identity(), "ident1")
    val ident2 = pipeline.addAtomic(new Identity(), "ident2")
    val consumer = graph.addAtomic(new Sink(), "consumer")

    graph.addEdge(producer, "result", pipeline, "source")
    graph.addEdge(pipeline, "source", ident1, "source")
    graph.addEdge(ident1, "result", ident2, "source")
    graph.addEdge(ident2, "result", pipeline.end, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    graph.close()
    assert(graph.valid)
  }

  "Containers " should " be able to read from nodes outside them" in {
    val graph = new Graph()

    val pipeline = graph.addPipeline()
    val producer = pipeline.addAtomic(new Producer(List("DOCUMENT")), "producer")
    val group    = pipeline.addGroup("group")
    val inner    = group.addAtomic(new Identity(), "ident")
    val consumer = graph.addAtomic(new Sink(), "consumer")

    graph.addEdge(producer, "result", inner, "source")
    graph.addEdge(inner, "result", group.end, "result")
    graph.addEdge(group, "result", pipeline.end, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    graph.close()
    assert(graph.valid)
  }

  "Steps " should " not be able to read from nodes inside containers" in {
    val graph = new Graph()

    val pipeline = graph.addPipeline()
    val producer = pipeline.addAtomic(new Producer(List("DOCUMENT")), "producer")
    val group    = pipeline.addGroup("group")
    val inner    = group.addAtomic(new Identity(), "inner")
    val outer    = pipeline.addAtomic(new Identity(), "outer")
    val consumer = graph.addAtomic(new Sink(), "consumer")

    graph.addEdge(producer, "result", inner, "source")
    graph.addEdge(inner, "result", group.end, "result")
    graph.addEdge(group, "result", pipeline.end, "result")
    graph.addEdge(inner, "result", outer, "source")
    graph.addEdge(outer, "result", pipeline.end, "result")
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
    val graph1 = new Graph()
    val graph2 = new Graph()

    val pipeline1 = graph1.addPipeline()
    val ident1 = pipeline1.addAtomic(new Identity(), "ident1")

    val pipeline2 = graph2.addPipeline()
    val ident2 = pipeline2.addAtomic(new Identity(), "ident2")

    var pass = false
    try {
      graph1.addEdge(ident1, "result", ident2, "source")
    } catch {
      case eg: GraphException => pass = true
    }

    assert(pass)
  }

  "A closed graph " should " not be updatable" in {
    val graph = new Graph()

    val pipeline = graph.addPipeline()
    val producer = graph.addAtomic(new Producer(List("DOCUMENT")), "producer")
    val ident1 = pipeline.addAtomic(new Identity(), "ident1")
    val ident2 = pipeline.addAtomic(new Identity(), "ident2")
    val consumer = graph.addAtomic(new Sink(), "consumer")

    graph.addEdge(producer, "result", pipeline, "source")
    graph.addEdge(pipeline, "source", ident1, "source")
    graph.addEdge(ident1, "result", ident2, "source")
    graph.addEdge(ident2, "result", pipeline.end, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    graph.close()

    var pass = false
    try {
      graph.addEdge(ident1, "result", ident2, "source")
    } catch {
      case ge: GraphException => pass = true
    }

    assert(pass)
  }

  "No loops " should " be allowed" in {
    val graph = new Graph()

    val pipeline = graph.addPipeline()
    val producer = graph.addAtomic(new Producer(List("DOCUMENT")), "producer")
    val ident1 = pipeline.addAtomic(new Identity(), "ident1")
    val ident2 = pipeline.addAtomic(new Identity(), "ident2")
    val consumer = graph.addAtomic(new Sink(), "consumer")

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