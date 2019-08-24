package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{BufferSink, Manifold, PortCardinality, Producer, RaiseError}
import org.scalatest.FlatSpec

class ExceptionSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  behavior of "An exception"

  it should "cause the pipeline to fail" in {
    val bc = new BufferSink()

    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(new Manifold(Manifold.WILD, Manifold.singlePort("result", PortCardinality.EXACTLY_ONE)))
    val p1       = pipeline.addAtomic(new Producer(List("doc1")), "p1")
    val error    = pipeline.addAtomic(new RaiseError("e2"), "e2")

    graph.addEdge(p1, "result", error, "source")
    graph.addEdge(error, "result", pipeline, "result")
    graph.addOutput(pipeline, "result")

    var pass = false
    try {
      val runtime = new GraphRuntime(graph, runtimeConfig)
      runtime.outputs("result").setConsumer(bc)
      runtime.run()
    } catch {
      case _: Exception =>
        pass = true
    }

    assert(pass)
  }
}