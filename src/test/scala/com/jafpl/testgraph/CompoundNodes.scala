package com.jafpl.testgraph

import com.jafpl.graph.Graph
import net.sf.saxon.s9api.Processor
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CompoundNodes extends FlatSpec {
  val processor = new Processor(false)

  "A group node" should "be created" in {
    val graph = new Graph()

    val childNode = graph.createNode()
    val groupNode = graph.createGroupNode(List(childNode))

    val valid = graph.valid()
    if (!valid) {
      throw new IllegalStateException("The graph isn't valid")
    }

    val correct = Array("A", "B", "C",
      "B.!latch_1 -> A.!latch"
    )
    val lines = graph.topology()

    var count = 0
    for (line <- lines) {
      assert(line == correct(count))
      count += 1
    }
  }

  "A group node with edges" should "be created" in {
    val graph = new Graph()

    val nodeA = graph.createNode()
    val nodeB = graph.createNode()
    val nodeC = graph.createNode()
    val groupNode = graph.createGroupNode(List(nodeB))
    graph.addEdge(nodeA, "result", nodeB, "source")
    graph.addEdge(nodeB, "result", groupNode.compoundEnd, "I_result")
    graph.addEdge(groupNode.compoundEnd, "result", nodeC, "source")

    val valid = graph.valid()
    if (!valid) {
      throw new IllegalStateException("The graph isn't valid")
    }

    val correct = Array("A", "B", "C", "D", "E",
      "A.result -> B.source",
      "B.result -> E.I_result",
      "E.result -> C.source")
    val lines = graph.topology()

    var count = 0
    for (line <- lines) {
      assert(line == correct(count))
      count += 1
    }
  }
}