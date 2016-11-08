package com.jafpl.drivers

import java.io.FileWriter

import com.jafpl.calc.AddExpr
import com.jafpl.graph.{Graph, LoopStart, Node}
import com.jafpl.steps.{GenerateLiteral, IterateIntegers}
import net.sf.saxon.s9api._

object LoopDemo2 extends App {
  val processor = new Processor(false)
  val graph = new Graph()

  val dumpGraph = Some("pg.xml")

  val input = graph.createNode(new GenerateLiteral(100))
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
    halt("Graph isn't valid?")
  }

  if (dumpGraph.isDefined) {
    if (dumpGraph.get == "") {
      println(graph.dump())
    } else {
      val pw = new FileWriter(dumpGraph.get)
      pw.write(graph.dump())
      pw.close()
    }
  }

  val runtime = graph.runtime
  runtime.run()
  runtime.waitForPipeline()

  var item = output.read()
  while (item.isDefined) {
    println(item.get)
    item = output.read()
  }

  runtime.teardown()

  def linkFrom(node: Node): Node = {
    node match {
      case l: LoopStart => l.compoundEnd
      case _ => node
    }
  }

  def halt(msg: String): Unit = {
    println(msg)
    System.exit(0)
  }
}
