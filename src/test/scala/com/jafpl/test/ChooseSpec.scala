package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.exceptions.JafplException
import com.jafpl.graph.Graph
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Identity, Producer}
import org.scalatest.FlatSpec

class ChooseSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "Only a when " should " should be allowed in a choose" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline = graph.addPipeline(None)
    val choose = pipeline.addChoose("choose")
    var pass = false
    try {
      choose.addAtomic(new Identity(), "identity")
    } catch {
      case eg: JafplException => pass = true
    }
    assert(pass)

    pass = false
    try {
      choose.addChoose("subchoose")
    } catch {
      case eg: JafplException => pass = true
    }
    assert(pass)

    pass = false
    try {
      choose.addGroup("group")
    } catch {
      case eg: JafplException => pass = true
    }
    assert(pass)
  }

  "A pipeline with choose first " should " choose the first" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline = graph.addPipeline(None)
    val producer = pipeline.addAtomic(new Producer(List("SomeDocument")), "producer")
    val choose = pipeline.addChoose("choose")
    val when1 = choose.addWhen("true", "when1")
    val when2 = choose.addWhen("false", "when2")

    val p1 = when1.addAtomic(new Producer(List("WHEN1")), "p1")
    val p2 = when2.addAtomic(new Producer(List("WHEN2")), "p2")

    val bc = new BufferSink()
    val consumer = pipeline.addAtomic(bc, "finalconsumer")

    graph.addEdge(producer, "result", when1, "condition")
    graph.addEdge(producer, "result", when2, "condition")

    graph.addEdge(p1, "result", when1, "result")
    graph.addEdge(p2, "result", when2, "result")

    graph.addEdge(when1, "result", choose, "result")
    graph.addEdge(when2, "result", choose, "result")

    graph.addEdge(choose, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "WHEN1")
  }

  "A pipeline with choose second " should " choose the second" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline = graph.addPipeline(None)
    val producer = pipeline.addAtomic(new Producer(List("SomeDocument")), "producer")
    val choose = pipeline.addChoose("choose")
    val when1 = choose.addWhen("false", "when1")
    val when2 = choose.addWhen("true", "when2")

    val p1 = when1.addAtomic(new Producer(List("WHEN1")), "p1")
    val p2 = when2.addAtomic(new Producer(List("WHEN2")), "p2")

    val bc = new BufferSink()
    val consumer = pipeline.addAtomic(bc, "finalconsumer")

    graph.addEdge(producer, "result", when1, "condition")
    graph.addEdge(producer, "result", when2, "condition")

    graph.addEdge(p1, "result", when1, "result")
    graph.addEdge(p2, "result", when2, "result")

    graph.addEdge(when1, "result", choose, "result")
    graph.addEdge(when2, "result", choose, "result")

    graph.addEdge(choose, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "WHEN2")
  }

  "A pipeline with choose both " should " choose the first" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline = graph.addPipeline(None)
    val producer = pipeline.addAtomic(new Producer(List("SomeDocument")), "producer")
    val choose = pipeline.addChoose("choose")
    val when1 = choose.addWhen("true", "when1")
    val when2 = choose.addWhen("true", "when2")

    val p1 = when1.addAtomic(new Producer(List("WHEN1")), "p1")
    val p2 = when2.addAtomic(new Producer(List("WHEN2")), "p2")

    val bc = new BufferSink()
    val consumer = pipeline.addAtomic(bc, "finalconsumer")

    graph.addEdge(producer, "result", when1, "condition")
    graph.addEdge(producer, "result", when2, "condition")

    graph.addEdge(p1, "result", when1, "result")
    graph.addEdge(p2, "result", when2, "result")

    graph.addEdge(when1, "result", choose, "result")
    graph.addEdge(when2, "result", choose, "result")

    graph.addEdge(choose, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "WHEN1")
  }

  "A pipeline with choose neither " should " choose neither" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline = graph.addPipeline(None)
    val producer = pipeline.addAtomic(new Producer(List("SomeDocument")), "producer")
    val choose = pipeline.addChoose("choose")
    val when1 = choose.addWhen("false", "when1")
    val when2 = choose.addWhen("false", "when2")

    val p1 = when1.addAtomic(new Producer(List("WHEN1")), "p1")
    val p2 = when2.addAtomic(new Producer(List("WHEN2")), "p2")

    val bc = new BufferSink()
    val consumer = pipeline.addAtomic(bc, "finalconsumer")

    graph.addEdge(producer, "result", when1, "condition")
    graph.addEdge(producer, "result", when2, "condition")

    graph.addEdge(p1, "result", when1, "result")
    graph.addEdge(p2, "result", when2, "result")

    graph.addEdge(when1, "result", choose, "result")
    graph.addEdge(when2, "result", choose, "result")

    graph.addEdge(choose, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.isEmpty)
  }
}