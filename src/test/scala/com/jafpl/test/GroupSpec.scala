package com.jafpl.test

import com.jafpl.config.Jafpl
import com.jafpl.graph.Graph
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{Identity, Manifold, Producer, Sink}
import org.scalatest.FlatSpec

class GroupSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "A group " should " run" in {
    val graph    = Jafpl.newInstance().newGraph()
    val pipeline = graph.addPipeline(Manifold.ALLOW_ANY)
    val producer = pipeline.addAtomic(new Producer(List("DOCUMENT")), "doc")
    val group    = pipeline.addGroup("group", Manifold.ALLOW_ANY)
    val ident    = group.addAtomic(new Identity(), "ident")
    val sink     = pipeline.addAtomic(new Sink(), "sink")

    graph.addEdge(producer, "result", ident, "source")
    graph.addEdge(ident, "result", group, "result")
    graph.addEdge(group, "result", sink, "source")

    //println(graph.asXML)
    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()
  }
}