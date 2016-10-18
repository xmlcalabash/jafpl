package com.jafpl.drivers

import java.io.FileWriter

import com.jafpl.graph.{Graph, LoopStart, Node}
import com.jafpl.steps.{Doubler, FlipSign, GenerateLiteral, WhenSigned}
import net.sf.saxon.s9api._

object ChooseDemo extends App {
  val processor = new Processor(false)
  val graph = new Graph()

  val dumpGraph = Some("pg.xml")

  val input = graph.createNode(new GenerateLiteral(0))
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
