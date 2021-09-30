package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.exceptions.JafplException
import com.jafpl.io.BufferConsumer
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{Identity, Manifold, RaiseError}
import org.scalatest.flatspec.AnyFlatSpec

class ForLoopSpec extends AnyFlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  behavior of "A for-loop"

  it should "iterate up" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline("mypipe", Manifold.ALLOW_ANY)
    val forloop  = pipeline.addFor("loop", 1, 10, Manifold.ALLOW_ANY)
    val ident = forloop.addAtomic(new Identity(), "ident")

    graph.addEdge(forloop, "current", ident, "source")
    graph.addEdge(ident, "result", forloop, "result")
    graph.addEdge(forloop, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

    assert(bc.items.size == 10)
    var count = 1
    for (buf <- bc.items) {
      assert(buf.toString == count.toString)
      count += 1
    }
  }

  it should "iterate down" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline("mypipe", Manifold.ALLOW_ANY)
    val forloop  = pipeline.addFor("loop", 20, 1, -2, Manifold.ALLOW_ANY)
    val ident = forloop.addAtomic(new Identity(), "ident")

    graph.addEdge(forloop, "current", ident, "source")
    graph.addEdge(ident, "result", forloop, "result")
    graph.addEdge(forloop, "result", pipeline, "result")

    graph.addOutput(pipeline, "result")

    graph.close()

    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    runtime.runSync()

    assert(bc.items.size == 10)
    var count = 20
    for (buf <- bc.items) {
      assert(buf.toString == count.toString)
      count -= 2
    }
  }

  it should "not iterate" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline("mypipe", Manifold.ALLOW_ANY)
    val forloop  = pipeline.addFor("loop", 1, 10, -1, Manifold.ALLOW_ANY)
    val error = forloop.addAtomic(new RaiseError("bang"), "ident")

    graph.addEdge(forloop, "current", error, "source")
    graph.addEdge(error, "result", forloop, "result")
    graph.addEdge(forloop, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    graph.close()

    var pass = false
    val runtime = new GraphRuntime(graph, runtimeConfig)
    val bc = new BufferConsumer()
    runtime.outputs("result").setConsumer(bc)
    try {
      runtime.runSync()
    } catch {
      case ex: JafplException =>
        assert(ex.code == JafplException.INVALID_LOOP_BOUNDS)
        pass = true
      case ex: Throwable =>
        fail()
    }
    assert(pass)
  }
}