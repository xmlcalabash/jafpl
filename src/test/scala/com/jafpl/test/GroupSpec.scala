package com.jafpl.test

import com.jafpl.graph.Graph
import com.jafpl.items.NumberItem
import com.jafpl.steps.{Doubler, GenerateLiteral}
import net.sf.saxon.s9api.Processor
import org.junit.runner.RunWith
import org.scalatest._
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class GroupSpec extends FlatSpec {
  val processor = new Processor(false)

  "A Group" should "evaluate the subpipeline" in {
    val graph = new Graph()

    val inputNumber = 4

    val input = graph.createNode(new GenerateLiteral(inputNumber))
    val output = graph.createOutputNode("OUTPUT")

    val double = graph.createNode(new Doubler())

    val group = graph.createGroupNode(List(double))

    graph.addEdge(input, "result", double, "source")

    graph.addEdge(double, "result", group.compoundEnd, "I_result")
    graph.addEdge(group.compoundEnd, "result", output, "source")

    val valid = graph.valid()
    if (!valid) {
      throw new IllegalStateException("The graph isn't valid")
    }

    val runtime = graph.runtime
    runtime.run()
    runtime.waitForPipeline()

    var count = 1
    var item = output.read()
    while (item.isDefined) {
      assert(count === 1)
      item.get match {
        case n: NumberItem =>
          assert(n.get === 2 * inputNumber)
        case _ =>
          assert(false)
      }
      count += 1
      item = output.read()
    }
  }
}