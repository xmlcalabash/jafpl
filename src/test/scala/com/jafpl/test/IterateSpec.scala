package com.jafpl.test

import com.jafpl.calc.AddExpr
import com.jafpl.graph.{Graph, OutputNode}
import com.jafpl.items.NumberItem
import com.jafpl.steps.{Doubler, GenerateLiteral, IterateIntegers}
import net.sf.saxon.s9api.Processor
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class IterateSpec extends FlatSpec {
  val processor = new Processor(false)

  "Iterating from 1 to 4 with a doubler" should "produce 2,4,6,8" in {
    val output = runDoublerGraph(4)

    var count = 1
    var item = output.read()
    while (item.isDefined) {
      assert(count <= 4)
      item.get match {
        case n: NumberItem =>
          assert(n.get === count * 2)
        case _ =>
          assert(false)
      }
      count += 1
      item = output.read()
    }
  }

  "Iterating from 1 to 100 with a doubler" should "produce 2,4,6,...,200" in {
    val output = runDoublerGraph(100)

    var count = 1
    var item = output.read()
    while (item.isDefined) {
      assert(count <= 100)
      item.get match {
        case n: NumberItem =>
          assert(n.get === count * 2)
        case _ =>
          assert(false)
      }
      count += 1
      item = output.read()
    }
  }

  "Iterating from 1 to 4 with an add one" should "produce 2,3,4,5" in {
    val output = runAddOneGraph(4)

    var count = 1
    var item = output.read()
    while (item.isDefined) {
      assert(count <= 4)
      item.get match {
        case n: NumberItem =>
          assert(n.get === count + 1)
        case _ =>
          assert(false)
      }
      count += 1
      item = output.read()
    }
  }

  "Iterating from 1 to 100 with an add one" should "produce 2,3,4,5,...,101" in {
    val output = runAddOneGraph(100)

    var count = 1
    var item = output.read()
    while (item.isDefined) {
      assert(count <= 100)
      item.get match {
        case n: NumberItem =>
          assert(n.get === count + 1)
        case _ =>
          assert(false)
      }
      count += 1
      item = output.read()
    }
  }

  def runDoublerGraph(max: Int): OutputNode = {
    val graph = new Graph()

    val input = graph.createInputNode("source")
    val output = graph.createOutputNode("OUTPUT")

    val double = graph.createNode(new Doubler())

    val loop = graph.createIteratorNode(new IterateIntegers(), List(double))

    graph.addEdge(input, "result", loop, "source")
    graph.addEdge(loop, "current", double, "source")
    graph.addEdge(double, "result", loop.compoundEnd, "I_result")
    graph.addEdge(loop.compoundEnd, "result", output, "source")

    val valid = graph.valid()
    if (!valid) {
      throw new IllegalStateException("The graph isn't valid")
    }

    val runtime = graph.runtime
    runtime.run()

    runtime.write("source", new NumberItem(max))
    runtime.close("source")

    runtime.waitForPipeline()

    output
  }

  def runAddOneGraph(max: Int): OutputNode = {
    val graph = new Graph()

    val input = graph.createNode(new GenerateLiteral(max))
    val output = graph.createOutputNode("OUTPUT")

    val increment = graph.createNode(new GenerateLiteral(1))

    val addto = graph.createNode(new AddExpr(List("+")))

    val loop = graph.createIteratorNode(new IterateIntegers(), List(addto))

    graph.addEdge(input, "result", loop, "source")
    graph.addEdge(loop, "current", addto, "s1")
    graph.addEdge(increment, "result", addto, "s2")
    graph.addEdge(addto, "result", loop.compoundEnd, "I_result")
    graph.addEdge(loop.compoundEnd, "result", output, "source")

    val valid = graph.valid()
    if (!valid) {
      throw new IllegalStateException("The graph isn't valid")
    }

    val runtime = graph.runtime
    runtime.run()

    runtime.write("source", new NumberItem(max))
    runtime.close("source")

    runtime.waitForPipeline()

    output
  }
}