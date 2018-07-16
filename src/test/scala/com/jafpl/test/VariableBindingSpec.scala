package com.jafpl.test

import java.io.{File, PrintWriter}

import com.jafpl.config.Jafpl
import com.jafpl.io.BufferConsumer
import com.jafpl.messages.{ItemMessage, Metadata}
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.ProduceBinding
import org.scalatest.FlatSpec

class VariableBindingSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "A variable binding " should " work" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline()

    val bind     = pipeline.addVariable("fred", "some value")
    val prodbind = pipeline.addAtomic(new ProduceBinding("fred"), "pb")

    graph.addBindingEdge(bind, prodbind)
    graph.addEdge(prodbind, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "some value")
  }

  "A variable binding provided by the runtime " should " also work" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline()

    val bind     = pipeline.addOption("fred", "some")
    val prodbind = pipeline.addAtomic(new ProduceBinding("fred"), "pb")

    graph.addBindingEdge(bind, prodbind)
    graph.addEdge(prodbind, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)

    runtime.setOption("fred", "hello world")

    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "hello world")
  }

  "A static variable binding " should " work" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline()

    val static   = new ItemMessage("static", Metadata.STRING)
    val bind     = pipeline.addVariable("fred", "some value", Some(static))
    val prodbind = pipeline.addAtomic(new ProduceBinding("fred"), "pb")

    graph.addBindingEdge(bind, prodbind)
    graph.addEdge(prodbind, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "static")
  }

  "An unreferenced unbound variable " should " be fine" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline()

    val bind_a   = pipeline.addOption("a", "")
    val bind_b   = pipeline.addOption("b", "")
    val prodbind = pipeline.addAtomic(new ProduceBinding("a"), "pb")

    graph.addBindingEdge(bind_a, prodbind)
    graph.addEdge(prodbind, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.setOption("a", "0")
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)

    var pass = true
    try {
      runtime.run()
    } catch {
      case t: Throwable =>
        println(t)
        pass = false
    }

    assert(pass)
  }

  "Intermediate variables " should " be computed" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline()

    val bind     = pipeline.addOption("a", "")

    val b        = pipeline.addVariable("b", "a + 1")
    val c        = pipeline.addVariable("c", "a + 2")
    val d        = pipeline.addVariable("d", "b + c")

    val prod     = pipeline.addAtomic(new ProduceBinding("d"), "pb")

    graph.addBindingEdge(bind, b)
    graph.addBindingEdge(bind, c)
    graph.addBindingEdge(b, d)
    graph.addBindingEdge(c, d)
    graph.addBindingEdge(d, prod)
    graph.addEdge(prod, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)

    runtime.setOption("a", "1")

    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == 5)
  }
}
