package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.exceptions.JafplException
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Identity, Manifold, Producer}
import org.scalatest.FlatSpec

class ChooseSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "Only a when " should " should be allowed in a choose" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline = graph.addPipeline(None, Manifold.ALLOW_ANY)
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
      choose.addGroup("group", Manifold.ALLOW_ANY)
    } catch {
      case eg: JafplException => pass = true
    }
    assert(pass)
  }

  "A pipeline with choose first " should " choose the first" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline = graph.addPipeline(None, Manifold.ALLOW_ANY)
    val producer = pipeline.addAtomic(new Producer(List("SomeDocument")), "producer")
    val choose = pipeline.addChoose("choose")
    val when1 = choose.addWhen("true", "when1", Manifold.ALLOW_ANY)
    val when2 = choose.addWhen("false", "when2", Manifold.ALLOW_ANY)

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

    val pipeline = graph.addPipeline(None, Manifold.ALLOW_ANY)
    val producer = pipeline.addAtomic(new Producer(List("SomeDocument")), "producer")
    val choose = pipeline.addChoose("choose")
    val when1 = choose.addWhen("false", "when1", Manifold.ALLOW_ANY)
    val when2 = choose.addWhen("true", "when2", Manifold.ALLOW_ANY)

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

    val pipeline = graph.addPipeline(None,Manifold.ALLOW_ANY)
    val producer = pipeline.addAtomic(new Producer(List("SomeDocument")), "producer")
    val choose = pipeline.addChoose("choose")
    val when1 = choose.addWhen("true", "when1", Manifold.ALLOW_ANY)
    val when2 = choose.addWhen("true", "when2", Manifold.ALLOW_ANY)

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

    val pipeline = graph.addPipeline(None, Manifold.ALLOW_ANY)
    val producer = pipeline.addAtomic(new Producer(List("SomeDocument")), "producer")
    val choose = pipeline.addChoose("choose")
    val when1 = choose.addWhen("false", "when1", Manifold.ALLOW_ANY)
    val when2 = choose.addWhen("false", "when2", Manifold.ALLOW_ANY)

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

  "A choose " should " stop after the first match" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline = graph.addPipeline(None, Manifold.ALLOW_ANY)
    val producer = pipeline.addAtomic(new Producer(List("SomeDocument")), "producer")
    val choose = pipeline.addChoose("choose")
    val when1 = choose.addWhen("false", "when1", Manifold.ALLOW_ANY)
    val when2 = choose.addWhen("false", "when2", Manifold.ALLOW_ANY)
    val when3 = choose.addWhen("false", "when3", Manifold.ALLOW_ANY)
    val when4 = choose.addWhen("false", "when4", Manifold.ALLOW_ANY)
    val when5 = choose.addWhen("true", "when5", Manifold.ALLOW_ANY)
    val when6 = choose.addWhen("true", "when6", Manifold.ALLOW_ANY)
    val when7 = choose.addWhen("true", "when7", Manifold.ALLOW_ANY)

    val p1 = when1.addAtomic(new Producer(List("WHEN1")), "p1")
    val p2 = when2.addAtomic(new Producer(List("WHEN2")), "p2")
    val p3 = when3.addAtomic(new Producer(List("WHEN3")), "p3")
    val p4 = when4.addAtomic(new Producer(List("WHEN4")), "p4")
    val p5 = when5.addAtomic(new Producer(List("WHEN5")), "p5")
    val p6 = when6.addAtomic(new Producer(List("WHEN6")), "p6")
    val p7 = when7.addAtomic(new Producer(List("WHEN7")), "p7")

    val bc = new BufferSink()
    val consumer = pipeline.addAtomic(bc, "finalconsumer")

    graph.addEdge(producer, "result", when1, "condition")
    graph.addEdge(producer, "result", when2, "condition")
    graph.addEdge(producer, "result", when3, "condition")
    graph.addEdge(producer, "result", when4, "condition")
    graph.addEdge(producer, "result", when5, "condition")
    graph.addEdge(producer, "result", when6, "condition")
    graph.addEdge(producer, "result", when7, "condition")

    graph.addEdge(p1, "result", when1, "result")
    graph.addEdge(p2, "result", when2, "result")
    graph.addEdge(p3, "result", when3, "result")
    graph.addEdge(p4, "result", when4, "result")
    graph.addEdge(p5, "result", when5, "result")
    graph.addEdge(p6, "result", when6, "result")
    graph.addEdge(p7, "result", when7, "result")

    graph.addEdge(when1, "result", choose, "result")
    graph.addEdge(when2, "result", choose, "result")
    graph.addEdge(when3, "result", choose, "result")
    graph.addEdge(when4, "result", choose, "result")
    graph.addEdge(when5, "result", choose, "result")
    graph.addEdge(when6, "result", choose, "result")
    graph.addEdge(when7, "result", choose, "result")

    graph.addEdge(choose, "result", pipeline, "result")
    graph.addEdge(pipeline, "result", consumer, "source")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "WHEN5")
  }
}