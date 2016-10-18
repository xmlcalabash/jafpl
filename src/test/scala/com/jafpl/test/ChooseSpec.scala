package com.jafpl.test

import com.jafpl.graph.{Graph, OutputNode}
import com.jafpl.items.{NumberItem, StringItem}
import com.jafpl.steps.{Doubler, FlipSign, GenerateLiteral, WhenSigned}
import net.sf.saxon.s9api.Processor
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ChooseSpec extends FlatSpec {

  val processor = new Processor(false)

  "A Choose with 0 " should "select the zero when" in {
    val output = runGraph(0)

    var count = 1
    var item = output.read()
    while (item.isDefined) {
      assert(count === 1)
      item.get match {
        case s: StringItem =>
          assert(s.get === "zero")
        case _ =>
          assert(false)
      }
      count += 1
      item = output.read()
    }
  }

  "A Choose with -10 " should "select the negative when" in {
    val output = runGraph(-10)

    var count = 1
    var item = output.read()
    while (item.isDefined) {
      assert(count === 1)
      item.get match {
        case n: NumberItem =>
          assert(n.get === -20)
        case _ =>
          assert(false)
      }
      count += 1
      item = output.read()
    }
  }

  "A Choose with +10 " should "select the positive when" in {
    val output = runGraph(10)

    var count = 1
    var item = output.read()
    while (item.isDefined) {
      assert(count === 1)
      item.get match {
        case n: NumberItem =>
          assert(n.get === -10)
        case _ =>
          assert(false)
      }
      count += 1
      item = output.read()
    }
  }

  def runGraph(inputNumber: Int): OutputNode = {
    val graph = new Graph()

    val input = graph.createNode(new GenerateLiteral(inputNumber))
    val output = graph.createOutputNode("OUTPUT")

    val double = graph.createNode(new Doubler())
    val flip = graph.createNode(new FlipSign())
    val zero = graph.createNode(new GenerateLiteral("zero"))

    val when1 = graph.createWhenNode(new WhenSigned(choosePos = false), List(double))
    val when2 = graph.createWhenNode(new WhenSigned(choosePos = true), List(flip))
    val other = graph.createWhenNode(List(zero))
    val choose = graph.createChooseNode(List(when1, when2, other))

    graph.addEdge(input, "result", when1, "condition")
    graph.addEdge(input, "result", when2, "condition")
    graph.addEdge(input, "result", other, "condition")

    graph.addEdge(input, "result", double, "source")
    graph.addEdge(input, "result", flip, "source")
    graph.addEdge(input, "result", zero, "source")

    graph.addEdge(when1.compoundEnd, "result", choose.compoundEnd, "I_result")
    graph.addEdge(when2.compoundEnd, "result", choose.compoundEnd, "I_result")
    graph.addEdge(other.compoundEnd, "result", choose.compoundEnd, "I_result")

    graph.addEdge(double, "result", when1.compoundEnd, "I_result")
    graph.addEdge(flip, "result", when2.compoundEnd, "I_result")
    graph.addEdge(zero, "result", other.compoundEnd, "I_result")

    graph.addEdge(choose.compoundEnd, "result", output, "source")

    val valid = graph.valid()
    if (!valid) {
      throw new IllegalStateException("The graph isn't valid")
    }

    val runtime = graph.runtime
    runtime.run()
    runtime.waitForPipeline()

    output
  }
}