package com.jafpl.testgraph

import com.jafpl.graph.Graph
import net.sf.saxon.s9api.Processor
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SimpleNodes extends FlatSpec {
  val processor = new Processor(false)

  "A single node" should "be created" in {
    val graph = new Graph()

    val node = graph.createNode()

    val valid = graph.valid()
    if (!valid) {
      throw new IllegalStateException("The graph isn't valid")
    }

    val lines = graph.topology()
    for (line <- lines) {
      assert(line == "A")
    }
  }

  "A single edge" should "be created" in {
    val graph = new Graph()

    val nodeA = graph.createNode()
    val nodeB = graph.createNode()
    graph.addEdge(nodeA, "result", nodeB, "source")

    val valid = graph.valid()
    if (!valid) {
      throw new IllegalStateException("The graph isn't valid")
    }

    val correct = Array("A", "B", "A.result -> B.source")
    val lines = graph.topology()

    var count = 0
    for (line <- lines) {
      assert(line == correct(count))
      count += 1
    }
  }

  "A fan out node" should "be created" in {
    val graph = new Graph()

    val nodeA = graph.createNode()
    val nodeB = graph.createNode()
    val nodeC = graph.createNode()
    graph.addEdge(nodeA, "result", nodeB, "source")
    graph.addEdge(nodeA, "result", nodeC, "source")

    val valid = graph.valid()
    if (!valid) {
      throw new IllegalStateException("The graph isn't valid")
    }

    val correct = Array("A", "B", "C", "D",
      "A.result -> D.source",
      "D.result_1 -> B.source",
      "D.result_2 -> C.source"
    )
    val lines = graph.topology()

    var count = 0
    for (line <- lines) {
      assert(line == correct(count))
      count += 1
    }
  }

  "A fan in node" should "be created" in {
    val graph = new Graph()

    val nodeA = graph.createNode()
    val nodeB = graph.createNode()
    val nodeC = graph.createNode()
    graph.addEdge(nodeA, "result", nodeC, "source")
    graph.addEdge(nodeB, "result", nodeC, "source")

    val valid = graph.valid()
    if (!valid) {
      throw new IllegalStateException("The graph isn't valid")
    }

    val correct = Array("A", "B", "C", "D",
      "A.result -> D.source_1",
      "B.result -> D.source_2",
      "D.result -> C.source"
    )
    val lines = graph.topology()

    var count = 0
    for (line <- lines) {
      assert(line == correct(count))
      count += 1
    }
  }
}