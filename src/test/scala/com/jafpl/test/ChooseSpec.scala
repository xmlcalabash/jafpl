package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.exceptions.JafplException
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Identity, Manifold, Producer}
import org.scalatest.flatspec.AnyFlatSpec

class ChooseSpec extends AnyFlatSpec {
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
      case _: JafplException => pass = true
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

    graph.addEdge(producer, "result", when1, "condition")
    graph.addEdge(producer, "result", when2, "condition")

    graph.addEdge(p1, "result", when1, "result")
    graph.addEdge(p2, "result", when2, "result")

    graph.addEdge(when1, "result", choose, "result")
    graph.addEdge(when2, "result", choose, "result")

    graph.addEdge(choose, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

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

    graph.addEdge(producer, "result", when1, "condition")
    graph.addEdge(producer, "result", when2, "condition")

    graph.addEdge(p1, "result", when1, "result")
    graph.addEdge(p2, "result", when2, "result")

    graph.addEdge(when1, "result", choose, "result")
    graph.addEdge(when2, "result", choose, "result")

    graph.addEdge(choose, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

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

    graph.addEdge(producer, "result", when1, "condition")
    graph.addEdge(producer, "result", when2, "condition")

    graph.addEdge(p1, "result", when1, "result")
    graph.addEdge(p2, "result", when2, "result")

    graph.addEdge(when1, "result", choose, "result")
    graph.addEdge(when2, "result", choose, "result")

    graph.addEdge(choose, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

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

    graph.addEdge(producer, "result", when1, "condition")
    graph.addEdge(producer, "result", when2, "condition")

    graph.addEdge(p1, "result", when1, "result")
    graph.addEdge(p2, "result", when2, "result")

    graph.addEdge(when1, "result", choose, "result")
    graph.addEdge(when2, "result", choose, "result")

    graph.addEdge(choose, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

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
    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

    assert(bc.items.size == 1)
    assert(bc.items.head == "WHEN5")
  }

  "A simple test pipeline " should " support simple numeric comparisons" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline = graph.addPipeline(None, Manifold.ALLOW_ANY)
    val producer = pipeline.addAtomic(new Producer(List(17)), "producer")
    val choose = pipeline.addChoose("choose")
    val when1 = choose.addWhen(". != ", "when1", Manifold.ALLOW_ANY)

    val p1 = when1.addAtomic(new Producer(List("WHEN1")), "p1")

    val bc = new BufferSink()

    graph.addEdge(producer, "result", when1, "condition")

    graph.addEdge(p1, "result", when1, "result")

    graph.addEdge(when1, "result", choose, "result")

    graph.addEdge(choose, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

    assert(bc.items.size == 1)
    assert(bc.items.head == "WHEN1")
  }

  "A test pipeline " should " support simple numeric comparisons" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline = graph.addPipeline(None, Manifold.ALLOW_ANY)
    val producer = pipeline.addAtomic(new Producer(List(1)), "producer")
    val choose = pipeline.addChoose("choose")
    val when1 = choose.addWhen(". > 2", "when1", Manifold.ALLOW_ANY)
    val when2 = choose.addWhen(". > 0", "when2", Manifold.ALLOW_ANY)
    val when3 = choose.addWhen("true", "when3", Manifold.ALLOW_ANY)

    val p1 = when1.addAtomic(new Producer(List("WHEN1")), "p1")
    val p2 = when2.addAtomic(new Producer(List("WHEN2")), "p2")
    val p3 = when3.addAtomic(new Producer(List("WHEN3")), "p3")

    val bc = new BufferSink()

    graph.addEdge(producer, "result", when1, "condition")
    graph.addEdge(producer, "result", when2, "condition")
    graph.addEdge(producer, "result", when3, "condition")

    graph.addEdge(p1, "result", when1, "result")
    graph.addEdge(p2, "result", when2, "result")
    graph.addEdge(p3, "result", when3, "result")

    graph.addEdge(when1, "result", choose, "result")
    graph.addEdge(when2, "result", choose, "result")
    graph.addEdge(when3, "result", choose, "result")

    graph.addEdge(choose, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

    assert(bc.items.size == 1)
    assert(bc.items.head == "WHEN2")
  }

  "A choose inside a short loop " should " work correctly" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val producer = pipeline.addAtomic(new Producer(List(1)), "producer")
    val forEach  = pipeline.addForEach("for-each", Manifold.ALLOW_ANY)

    val choose = forEach.addChoose("choose")
    val when3 = choose.addWhen("true", "when3", Manifold.ALLOW_ANY)

    val p3 = when3.addAtomic(new Producer(List("WHEN1")), "p3")

    val p3i = when3.addAtomic(new Identity(true, "P3"), "p3i")

    val bc = new BufferSink()

    graph.addEdge(producer, "result", forEach, "source")

    graph.addEdge(forEach, "current", when3, "condition")

    graph.addEdge(p3, "result", p3i, "source")
    graph.addEdge(p3i, "result", when3, "result")

    graph.addEdge(when3, "result", choose, "result")

    graph.addEdge(choose, "result", forEach, "result")

    graph.addEdge(forEach, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

    assert(bc.items.size == 1)
    assert(bc.items.head == "WHEN1")
  }

  "A choose inside a loop " should " work correctly" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val producer = pipeline.addAtomic(new Producer(List(1,2,3)), "producer")
    val forEach  = pipeline.addForEach("for-each", Manifold.ALLOW_ANY)

    val choose = forEach.addChoose("choose")
    val when1 = choose.addWhen(". > 2", "when1", Manifold.ALLOW_ANY)
    val when2 = choose.addWhen(". > 1", "when2", Manifold.ALLOW_ANY)
    val when3 = choose.addWhen("true", "when3", Manifold.ALLOW_ANY)

    val p1 = when1.addAtomic(new Producer(List("WHEN3")), "p1")
    val p3 = when3.addAtomic(new Producer(List("WHEN1")), "p3")

    val p1i = when1.addAtomic(new Identity(true, "P1"), "p1i")
    val p2i = when2.addAtomic(new Identity(true, "P2"), "p2i")
    val p3i = when3.addAtomic(new Identity(true, "P3"), "p3i")

    val bc = new BufferSink()

    graph.addEdge(producer, "result", forEach, "source")

    graph.addEdge(forEach, "current", when1, "condition")
    graph.addEdge(forEach, "current", when2, "condition")
    graph.addEdge(forEach, "current", when3, "condition")

    graph.addEdge(forEach, "current", p1i, "source")
    graph.addEdge(p1, "result", p1i, "source")
    graph.addEdge(p1i, "result", when1, "result")

    graph.addEdge(forEach, "current", p2i, "source")
    graph.addEdge(p2i, "result", when2, "result")

    graph.addEdge(p3, "result", p3i, "source")
    graph.addEdge(forEach, "current", p3i, "source")
    graph.addEdge(p3i, "result", when3, "result")

    graph.addEdge(when1, "result", choose, "result")
    graph.addEdge(when2, "result", choose, "result")
    graph.addEdge(when3, "result", choose, "result")

    graph.addEdge(choose, "result", forEach, "result")

    graph.addEdge(forEach, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

    print(bc.items)

    assert(bc.items.size == 5)
    assert(bc.items(0) == 1)
    assert(bc.items(1) == "WHEN1")
    assert(bc.items(2) == 2)
    assert(bc.items(3) == 3)
    assert(bc.items(4) == "WHEN3")
  }

  "If a test expression raises an error, the choose " should " fail" in {
    val graph    = Jafpl.newInstance().newGraph()

    val pipeline = graph.addPipeline(None, Manifold.ALLOW_ANY)
    val producer = pipeline.addAtomic(new Producer(List("SomeDocument")), "producer")
    val choose = pipeline.addChoose("choose")
    val when1 = choose.addWhen("ERROR", "when1", Manifold.ALLOW_ANY)
    val when2 = choose.addWhen("true", "when2", Manifold.ALLOW_ANY)

    val p1 = when1.addAtomic(new Producer(List("WHEN1")), "p1")
    val p2 = when2.addAtomic(new Producer(List("WHEN2")), "p2")

    val bc = new BufferSink()

    graph.addEdge(producer, "result", when1, "condition")
    graph.addEdge(producer, "result", when2, "condition")

    graph.addEdge(p1, "result", when1, "result")
    graph.addEdge(p2, "result", when2, "result")

    graph.addEdge(when1, "result", choose, "result")
    graph.addEdge(when2, "result", choose, "result")

    graph.addEdge(choose, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    val runtime = new GraphRuntime(graph, runtimeConfig)

    var pass = false
    try {
      runtime.outputs("result").setConsumer(bc)
      runtime.runSync()
    } catch {
      case _: Exception =>
        pass = true
    }

    assert(pass)
  }
}