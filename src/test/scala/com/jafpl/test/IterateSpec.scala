package com.jafpl.test

import com.jafpl.graph.{Graph, OutputNode}
import com.jafpl.items.NumberItem
import com.jafpl.steps.{Doubler, IterateIntegers}
import net.sf.saxon.s9api.Processor
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class IterateSpec extends FlatSpec {
  val processor = new Processor(false)

  "Iterating from 1 to 4 with a doubler" should "produce 2,4,6,8" in {
    val output = runGraph(4)

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

  def runGraph(max: Int): OutputNode = {
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
}