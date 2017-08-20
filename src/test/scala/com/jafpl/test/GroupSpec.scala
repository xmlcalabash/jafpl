package com.jafpl.test

import com.jafpl.graph.Graph
import com.jafpl.primitive.PrimitiveRuntimeConfiguration
import com.jafpl.runtime.GraphRuntime
import com.jafpl.steps.{Identity, Producer, Sink}
import org.scalatest.FlatSpec

class GroupSpec extends FlatSpec {
  var runtimeConfig = new PrimitiveRuntimeConfiguration()

  "A group " should " run" in {
    val graph = new Graph()
    val pipeline = graph.addPipeline()
    val producer = pipeline.addAtomic(new Producer(List("DOCUMENT")), "doc")
    val group    = pipeline.addGroup("group")
    val ident    = group.addAtomic(new Identity(), "ident")
    val sink     = pipeline.addAtomic(new Sink(), "sink")

    graph.addEdge(producer, "result", ident, "source")
    graph.addEdge(ident, "result", group.end, "result")
    graph.addEdge(group, "result", sink, "source")

    println(graph.asXML)
    val runtime = new GraphRuntime(graph, runtimeConfig)
    runtime.run()
  }
}