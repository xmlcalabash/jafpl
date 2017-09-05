package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.exceptions.PipelineException
import com.jafpl.graph.Graph
import com.jafpl.io.BufferConsumer
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.ProduceBinding
import org.scalatest.FlatSpec

class VariableBindings extends FlatSpec {
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

    val bind     = graph.addBinding("fred")
    val prodbind = pipeline.addAtomic(new ProduceBinding("fred"), "pb")

    graph.addBindingEdge(bind, prodbind)
    graph.addEdge(prodbind, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)

    runtime.bindings("fred").set("hello world")

    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == "hello world")
  }

  "An unbound variable " should " cause an exception" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline()

    val bind     = graph.addBinding("fred")
    val prodbind = pipeline.addAtomic(new ProduceBinding("fred"), "pb")

    graph.addBindingEdge(bind, prodbind)
    graph.addEdge(prodbind, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)

    var pass = false
    try {
      runtime.run()
    } catch {
      case err: PipelineException =>
        pass = err.code == "nobinding"
      case _: Throwable => Unit
    }

    assert(pass)
  }

  "Intermediate variables " should " be computed" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline()

    val bind     = graph.addBinding("a")

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

    runtime.bindings("a").set("1")

    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.run()

    assert(bc.items.size == 1)
    assert(bc.items.head == 5)
  }

}